/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2011 Igalia, S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.libreplan.business.test.orders.daos;

import static org.junit.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.Date;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

import org.libreplan.business.calendars.daos.IBaseCalendarDAO;
import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.common.exceptions.ValidationException;
import org.libreplan.business.externalcompanies.entities.DeadlineCommunication;
import org.libreplan.business.orders.daos.IOrderDAO;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderStatusEnum;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.scenarios.bootstrap.IScenariosBootstrap;
import org.libreplan.business.scenarios.entities.OrderVersion;
import org.libreplan.business.test.calendars.entities.BaseCalendarTest;
import org.libreplan.business.test.planner.daos.ResourceAllocationDAOTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test for {@link IOrderDAO}.
 *
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE, BUSINESS_SPRING_CONFIG_TEST_FILE })
public class OrderDAOTest {

    @Before
    public void loadRequiredData() {
        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                scenariosBootstrap.loadRequiredData();
                return null;
            }
        });
    }

    @Autowired
    private IOrderDAO orderDAO;

    @Autowired
    private IBaseCalendarDAO calendarDAO;

    @Autowired
    private IScenariosBootstrap scenariosBootstrap;

    @Autowired
    private IScenarioManager scenarioManager;

    @Autowired
    private IAdHocTransactionService transactionService;

    @Test
    @Transactional
    public void testInSpringContainer() {
        assertNotNull(orderDAO);
    }

    private Order createValidOrder(String name) {
        Order order = Order.create();
        order.setName(name);
        order.setCode(UUID.randomUUID().toString());
        order.setInitDate(new Date());
        BaseCalendar basicCalendar = BaseCalendarTest.createBasicCalendar();
        calendarDAO.save(basicCalendar);
        order.setCalendar(basicCalendar);
        OrderVersion orderVersion = ResourceAllocationDAOTest.setupVersionUsing(scenarioManager, order);
        order.useSchedulingDataFor(orderVersion);
        return order;
    }

    private Order createValidOrderWithDeadlineCommunications(String name) {
        Order order = createValidOrder(name);

        // Create two deadline communications
        Date date1 = (new Date());
        Date date2 = (new LocalDate(date1).plusDays(3)).toDateTimeAtStartOfDay().toDate();

        DeadlineCommunication deadlineCommunication1 = DeadlineCommunication.create(date1, null);
        DeadlineCommunication deadlineCommunication2 = DeadlineCommunication.create(date2, null);

        order.getDeliveringDates().add(deadlineCommunication1);
        order.getDeliveringDates().add(deadlineCommunication2);

        return order;
    }

    @Test
    @Transactional
    public void testSaveOrdersWithDeliveringDates() {
        Order order = createValidOrderWithDeadlineCommunications("test");
        orderDAO.save(order);
        orderDAO.flush();

        assertThat(order.getDeliveringDates().size(), equalTo(2));

        DeadlineCommunication dcFirst = order.getDeliveringDates().first();
        DeadlineCommunication dcLast = order.getDeliveringDates().last();

        assertTrue(dcFirst.getSaveDate().after(dcLast.getSaveDate()));


        // A new DeadlineCommunication is placed between the existing communications
        Date date = (new LocalDate(dcLast.getSaveDate()).plusDays(2)).toDateTimeAtStartOfDay().toDate();
        DeadlineCommunication deadlineCommunication = DeadlineCommunication.create(date, null);
        order.getDeliveringDates().add(deadlineCommunication);

        orderDAO.save(order);
        orderDAO.flush();

        assertThat(order.getDeliveringDates().size(), equalTo(3));

        dcFirst = order.getDeliveringDates().first();
        dcLast =  order.getDeliveringDates().last();
        DeadlineCommunication new_dc = (DeadlineCommunication) order.getDeliveringDates().toArray()[1];

        assertTrue(dcFirst.getSaveDate().after(dcLast.getSaveDate()));
        assertTrue(dcFirst.getSaveDate().after(new_dc.getSaveDate()));
        assertFalse(dcLast.equals(new_dc));
        assertTrue(dcLast.getSaveDate().before(new_dc.getSaveDate()));
    }

    @Test
    @Transactional
    public void testSaveTwoOrdersWithDifferentNames() {
        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                Order order = createValidOrder("test");
                orderDAO.save(order);
                orderDAO.flush();
                return null;
            }
        });

        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                Order order = createValidOrder("test2");
                orderDAO.save(order);
                orderDAO.flush();
                return null;
            }
        });
    }

    @Test(expected = ValidationException.class)
    @Transactional
    public void testSaveTwoOrdersWithSameNames() {
        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                Order order = createValidOrder("test");
                orderDAO.save(order);
                orderDAO.flush();
                return null;
            }
        });

        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                Order order = createValidOrder("test");
                orderDAO.save(order);
                orderDAO.flush();
                return null;
            }
        });
    }

    /*
     * Characterization tests added for the Hibernate Criteria -> JPA Criteria API migration
     * (Jakarta EE / Hibernate 6). findAll/findByCode/getActiveOrders/
     * getOrdersWithNotEmptyCustomersReferences/existsByNameAnotherTransaction/
     * findByNameAnotherTransaction had no test coverage before.
     */

    @Test
    @Transactional
    public void testFindAllIncludesSaved() {
        Order order = createValidOrder("test-" + UUID.randomUUID());
        orderDAO.save(order);

        boolean found = false;
        for (Order o : orderDAO.findAll()) {
            if (o.getId().equals(order.getId())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    @Transactional
    public void testFindByCodeIsCaseInsensitiveAndTrims() throws InstanceNotFoundException {
        Order order = createValidOrder("test-" + UUID.randomUUID());
        String mixedCaseCode = "MiXeD-" + UUID.randomUUID();
        order.setCode(mixedCaseCode);
        orderDAO.save(order);

        assertEquals(order.getId(), orderDAO.findByCode(mixedCaseCode).getId());
        assertEquals(order.getId(), orderDAO.findByCode(mixedCaseCode.toLowerCase()).getId());
        assertEquals(order.getId(), orderDAO.findByCode("  " + mixedCaseCode + "  ").getId());
    }

    @Test(expected = InstanceNotFoundException.class)
    @Transactional
    public void testFindByCodeThrowsWhenNotFound() throws InstanceNotFoundException {
        orderDAO.findByCode("does-not-exist-" + UUID.randomUUID());
    }

    @Test
    @Transactional
    public void testGetActiveOrdersExcludesCancelledAndStored() {
        Order active = createValidOrder("test-" + UUID.randomUUID());
        orderDAO.save(active);

        Order cancelled = createValidOrder("test-" + UUID.randomUUID());
        cancelled.setState(OrderStatusEnum.CANCELLED);
        orderDAO.save(cancelled);

        Order stored = createValidOrder("test-" + UUID.randomUUID());
        stored.setState(OrderStatusEnum.STORED);
        orderDAO.save(stored);

        boolean activeFound = false;
        for (Order o : orderDAO.getActiveOrders()) {
            if (o.getId().equals(active.getId())) {
                activeFound = true;
            }
            assertFalse(o.getId().equals(cancelled.getId()));
            assertFalse(o.getId().equals(stored.getId()));
        }
        assertTrue(activeFound);
    }

    @Test
    @Transactional
    public void testGetOrdersWithNotEmptyCustomersReferencesExcludesNullAndEmpty() {
        Order withReference = createValidOrder("test-" + UUID.randomUUID());
        withReference.setCustomerReference("ref-" + UUID.randomUUID());
        orderDAO.save(withReference);

        Order withEmptyReference = createValidOrder("test-" + UUID.randomUUID());
        withEmptyReference.setCustomerReference("");
        orderDAO.save(withEmptyReference);

        Order withNullReference = createValidOrder("test-" + UUID.randomUUID());
        orderDAO.save(withNullReference);

        List<Order> result = orderDAO.getOrdersWithNotEmptyCustomersReferences();
        boolean found = false;
        for (Order o : result) {
            if (o.getId().equals(withReference.getId())) {
                found = true;
            }
            assertFalse(o.getId().equals(withEmptyReference.getId()));
            assertFalse(o.getId().equals(withNullReference.getId()));
        }
        assertTrue(found);
    }

    @Test
    public void testFindByNameAnotherTransactionReturnsMatch() throws InstanceNotFoundException {
        final String name = "test-" + UUID.randomUUID();

        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                Order order = createValidOrder(name);
                orderDAO.save(order);
                orderDAO.flush();
                return null;
            }
        });

        assertEquals(name, orderDAO.findByNameAnotherTransaction(name).getName());
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testFindByNameAnotherTransactionThrowsWhenNotFound() throws InstanceNotFoundException {
        orderDAO.findByNameAnotherTransaction("does-not-exist-" + UUID.randomUUID());
    }

    @Test
    public void testExistsByNameAnotherTransaction() {
        final String name = "test-" + UUID.randomUUID();

        transactionService.runOnAnotherTransaction(new IOnTransaction<Void>() {
            @Override
            public Void execute() {
                Order order = createValidOrder(name);
                orderDAO.save(order);
                orderDAO.flush();
                return null;
            }
        });

        assertTrue(orderDAO.existsByNameAnotherTransaction(name));
        assertFalse(orderDAO.existsByNameAnotherTransaction("does-not-exist-" + UUID.randomUUID()));
    }

}
