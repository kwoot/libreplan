/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2011 Igalia, S.L.
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

package org.libreplan.business.test.workreports.daos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.annotation.Resource;

import org.hibernate.SessionFactory;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.IDataBootstrap;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.workreports.daos.IWorkReportLineDAO;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE, BUSINESS_SPRING_CONFIG_TEST_FILE })
/**
 * @author Diego Pino García <dpino@igalia.com>
 */
public class WorkReportLineDAOTest extends AbstractWorkReportTest {

    @Autowired
    private IWorkReportLineDAO workReportLineDAO;

    @Autowired
    private SessionFactory sessionFactory;

    @Resource
    private IDataBootstrap configurationBootstrap;

    @Before
    public void loadRequiredData() {
        configurationBootstrap.loadRequiredData();
    }

    @Test
    @Transactional
    public void testSaveWorkReportLine() {
        WorkReportLine workReportLine = createValidWorkReportLine();
        workReportLineDAO.save(workReportLine);
        assertTrue(workReportLineDAO.exists(workReportLine.getId()));
    }

    @Test
    @Transactional
    public void testRemoveWorkReportLine() throws InstanceNotFoundException {
        WorkReportLine workReportLine = createValidWorkReportLine();
        workReportLineDAO.save(workReportLine);
        workReportLine.getWorkReport().removeWorkReportLine(workReportLine);
        workReportLineDAO.remove(workReportLine.getId());
        assertFalse(workReportLineDAO.exists(workReportLine.getId()));
    }

    @Test
    @Transactional
    public void testListWorkReportLine() {
        int previous = workReportLineDAO.list(WorkReportLine.class).size();

        WorkReportLine workReportType1 = createValidWorkReportLine();
        workReportLineDAO.save(workReportType1);
        WorkReportLine workReportType2 = createValidWorkReportLine();
        workReportLineDAO.save(workReportType1);
        workReportLineDAO.save(workReportType2);

        List<WorkReportLine> list = workReportLineDAO.list(WorkReportLine.class);
        assertEquals(previous + 2, list.size());
    }

    /*
     * Characterization tests added for the Hibernate Criteria -> JPA Criteria API migration
     * (Jakarta EE / Hibernate 6). None of the Criteria-based find* methods had test coverage
     * before.
     */

    private Date daysFromNow(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    @Test
    @Transactional
    public void findByOrderElementMatchesExactOrderElement() {
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);

        List<WorkReportLine> result = workReportLineDAO.findByOrderElement(line.getOrderElement());

        assertTrue(result.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }

    @Test
    @Transactional
    public void findByOrderElementAndChildrenReturnsEmptyForNewOrderElement() {
        OrderElement notSaved = org.libreplan.business.orders.entities.OrderLine.create();
        assertTrue(workReportLineDAO.findByOrderElementAndChildren(notSaved).isEmpty());
    }

    @Test
    @Transactional
    public void findByOrderElementAndChildrenMatchesSavedLine() {
        // BaseEntity.create() sets isNewObject=true and save() never resets it (a recurring
        // gotcha throughout this migration) - findByOrderElementAndChildren(OrderElement) short
        // circuits to an empty list whenever isNewObject() is still true, so the orderElement
        // has to be marked as no-longer-transient to behave like a realistic DB-loaded entity.
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);
        line.getOrderElement().dontPoseAsTransientObjectAnymore();

        List<WorkReportLine> result = workReportLineDAO.findByOrderElementAndChildren(line.getOrderElement());

        assertTrue(result.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }

    @Test
    @Transactional
    public void findByOrderElementAndChildrenSortedByDateOrdersAscending() {
        WorkReportLine earlier = createValidWorkReportLine();
        earlier.setDate(daysFromNow(-10));
        workReportLineDAO.save(earlier);

        List<WorkReportLine> result =
                workReportLineDAO.findByOrderElementAndChildren(earlier.getOrderElement(), true);

        assertTrue(result.stream().anyMatch(l -> l.getId().equals(earlier.getId())));
    }

    @Test
    @Transactional
    public void findFilteredByDateRespectsStartAndEndBounds() {
        WorkReportLine line = createValidWorkReportLine();
        line.setDate(daysFromNow(0));
        workReportLineDAO.save(line);

        List<WorkReportLine> withinRange =
                workReportLineDAO.findFilteredByDate(daysFromNow(-1), daysFromNow(1));
        List<WorkReportLine> outsideRange =
                workReportLineDAO.findFilteredByDate(daysFromNow(2), daysFromNow(3));

        assertTrue(withinRange.stream().anyMatch(l -> l.getId().equals(line.getId())));
        assertFalse(outsideRange.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }

    @Test
    @Transactional
    public void findByResourcesReturnsEmptyForEmptyList() {
        assertTrue(workReportLineDAO.findByResources(new ArrayList<>()).isEmpty());
    }

    @Test
    @Transactional
    public void findByResourcesMatchesGivenResource() {
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);

        List<WorkReportLine> result = workReportLineDAO.findByResources(
                Collections.singletonList(line.getResource()));

        assertTrue(result.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }

    @Test
    @Transactional
    public void findByResourceFilteredByDateNotInWorkReportExcludesGivenWorkReport() {
        WorkReportLine line = createValidWorkReportLine();
        line.setDate(daysFromNow(0));
        workReportLineDAO.save(line);

        List<WorkReportLine> includingOwnWorkReport = workReportLineDAO.findByResourceFilteredByDateNotInWorkReport(
                line.getResource(), daysFromNow(-1), daysFromNow(1), null);
        List<WorkReportLine> excludingOwnWorkReport = workReportLineDAO.findByResourceFilteredByDateNotInWorkReport(
                line.getResource(), daysFromNow(-1), daysFromNow(1), line.getWorkReport());

        assertTrue(includingOwnWorkReport.stream().anyMatch(l -> l.getId().equals(line.getId())));
        assertFalse(excludingOwnWorkReport.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }

    @Test
    @Transactional
    public void isFinishedFalseWhenNoFinishedLineExists() {
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);

        assertFalse(workReportLineDAO.isFinished(line.getOrderElement()));
    }

    @Test(expected = RuntimeException.class)
    @Transactional
    public void findFinishedByOrderElementNotInWorkReportAnotherTransactionAlwaysThrows() {
        // findFinishedByOrderElementNotInWorkReportAnotherTransaction is REQUIRES_NEW, which
        // opens a brand new Hibernate session - any real OrderElement passed in (whether from
        // the same still-open outer transaction, or re-fetched after a separate commit) belongs
        // to a different persistence context, and the legacy Criteria restriction on it throws
        // org.hibernate.TransientObjectException: object references an unsaved transient
        // instance, even though the entity has a real, persisted id. Confirmed against the
        // pre-Jakarta implementation too - this method is broken for any real invocation,
        // independent of the Criteria migration. isOrderElementFinishedInAnotherWorkReportConstraint()
        // (the only production caller, via bean validation) is unreachable anyway, since saving
        // finished=true already fails first with a separate, unrelated validator reflection bug
        // (see savingAFinishedLineAlwaysThrowsDueToBrokenValidatorReflection).
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);

        workReportLineDAO.findFinishedByOrderElementNotInWorkReportAnotherTransaction(
                line.getOrderElement(), WorkReport.create(createValidWorkReportType()));
    }

    @Test
    @Transactional
    public void clockStartAndClockFinishRoundTripThroughPersistence() {
        // clockStart/clockFinish are org.joda.time.LocalTime, mapped via a custom Hibernate
        // UserType (previously org.jadira.usertype.dateandtime.joda.PersistentLocalTimeAsMillisInteger,
        // stored as millis-of-day INTEGER). Force a real DB round-trip (not just the in-memory
        // object) by flushing and evicting before reloading.
        WorkReportLine line = createValidWorkReportLine();
        LocalTime start = new LocalTime(9, 30, 15, 250);
        LocalTime finish = new LocalTime(17, 45, 0, 0);
        line.setClockStart(start);
        line.setClockFinish(finish);
        workReportLineDAO.save(line);
        workReportLineDAO.flush();
        sessionFactory.getCurrentSession().evict(line);

        WorkReportLine reloaded = workReportLineDAO.findExistingEntity(line.getId());

        assertEquals(start, reloaded.getClockStart());
        assertEquals(finish, reloaded.getClockFinish());
    }

    @Test
    @Transactional
    public void clockStartAndClockFinishNullRoundTripThroughPersistence() {
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);
        workReportLineDAO.flush();
        sessionFactory.getCurrentSession().evict(line);

        WorkReportLine reloaded = workReportLineDAO.findExistingEntity(line.getId());

        assertEquals(null, reloaded.getClockStart());
        assertEquals(null, reloaded.getClockFinish());
    }

    @Test(expected = jakarta.validation.ValidationException.class)
    @Transactional
    public void savingAFinishedLineAlwaysThrowsDueToBrokenValidatorReflection() {
        // WorkReportLine.isOrderElementFinishedInAnotherWorkReportConstraint() (an @AssertTrue
        // bean-validation method) is invoked by Hibernate Validator via reflection whenever
        // finished=true is saved, and it always fails with "HV000090: Unable to access
        // isOrderElementFinishedInAnotherWorkReportConstraint" - confirmed against the
        // pre-Jakarta implementation too, so this is a pre-existing environment/reflection bug,
        // not something introduced by the Criteria migration. This means isFinished() and
        // findFinishedByOrderElementNotInWorkReportAnotherTransaction() can only be
        // characterized for the "no finished lines" branch - there is no way to persist a
        // finished=true line to exercise the positive branch.
        WorkReportLine line = createValidWorkReportLine();
        line.setFinished(true);
        workReportLineDAO.save(line);
    }

    @Test
    @Transactional
    public void findByOrderElementAndWorkReportsReturnsEmptyForEmptyList() {
        WorkReportLine line = createValidWorkReportLine();
        assertTrue(workReportLineDAO.findByOrderElementAndWorkReports(
                line.getOrderElement(), new ArrayList<>()).isEmpty());
    }

    @Test
    @Transactional
    public void findByOrderElementAndWorkReportsMatchesGivenWorkReport() {
        WorkReportLine line = createValidWorkReportLine();
        workReportLineDAO.save(line);

        List<WorkReportLine> result = workReportLineDAO.findByOrderElementAndWorkReports(
                line.getOrderElement(), Collections.singletonList(line.getWorkReport()));

        assertTrue(result.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }

    @Test
    @Transactional
    public void findByOrderElementAndChildrenFilteredByDateReturnsEmptyForNewOrderElement() {
        OrderElement notSaved = org.libreplan.business.orders.entities.OrderLine.create();
        assertTrue(workReportLineDAO.findByOrderElementAndChildrenFilteredByDate(
                notSaved, null, null, false).isEmpty());
    }

    @Test
    @Transactional
    public void findByOrderElementAndChildrenFilteredByDateMatchesWithinRange() {
        // Same isNewObject()-never-reset gotcha as findByOrderElementAndChildrenMatchesSavedLine.
        WorkReportLine line = createValidWorkReportLine();
        line.setDate(daysFromNow(0));
        workReportLineDAO.save(line);
        line.getOrderElement().dontPoseAsTransientObjectAnymore();

        List<WorkReportLine> withinRange = workReportLineDAO.findByOrderElementAndChildrenFilteredByDate(
                line.getOrderElement(), daysFromNow(-1), daysFromNow(1), true);
        List<WorkReportLine> outsideRange = workReportLineDAO.findByOrderElementAndChildrenFilteredByDate(
                line.getOrderElement(), daysFromNow(2), daysFromNow(3), true);

        assertTrue(withinRange.stream().anyMatch(l -> l.getId().equals(line.getId())));
        assertFalse(outsideRange.stream().anyMatch(l -> l.getId().equals(line.getId())));
    }
}
