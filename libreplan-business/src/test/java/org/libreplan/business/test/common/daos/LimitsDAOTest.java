/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2015 LibrePlan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.libreplan.business.test.common.daos;

import static org.junit.Assert.assertNull;
import static org.libreplan.business.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_FILE;
import static org.libreplan.business.test.BusinessGlobalNames.BUSINESS_SPRING_CONFIG_TEST_FILE;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.libreplan.business.common.daos.ILimitsDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { BUSINESS_SPRING_CONFIG_FILE,
        BUSINESS_SPRING_CONFIG_TEST_FILE })
/**
 * Characterization tests for {@link LimitsDAO}, written ahead of the
 * Hibernate Criteria -> JPA Criteria API migration (Jakarta EE / Hibernate 6)
 * to prove behavior is unchanged after the rewrite.
 *
 * Note: Limits.hbm.xml declares <class name="Limits" abstract="true" ...> - a
 * pre-existing bug unrelated to this migration. Hibernate's "increment" id
 * generator cannot initialize against an abstract-mapped class, so
 * ILimitsDAO.save() throws a SQLGrammarException for ANY Limits instance, on
 * both the old and new implementation. There is no working way to persist a
 * Limits row, so getAll()/getLimitsByType() can only be characterized on
 * their empty-database behavior.
 */
public class LimitsDAOTest {

    @Autowired
    private ILimitsDAO limitsDAO;

    @Test
    @Transactional
    public void testGetLimitsByTypeReturnsNullWhenAbsent() {
        assertNull(limitsDAO.getLimitsByType("does-not-exist-" + UUID.randomUUID()));
    }

}
