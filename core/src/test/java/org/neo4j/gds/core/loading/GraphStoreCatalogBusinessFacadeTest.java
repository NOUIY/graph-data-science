/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.core.loading;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.api.DatabaseId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphStoreCatalogBusinessFacadeTest {
    @Test
    void shouldDetermineGraphExists() {
        var service = mock(GraphStoreCatalogService.class);
        var facade = new GraphStoreCatalogBusinessFacade(
            mock(PreconditionsService.class),
            mock(GraphNameValidationService.class),
            service
        );

        when(service.graphExists("someUser", DatabaseId.from("someDatabase"), "someGraph")).thenReturn(true);
        boolean graphExists = facade.graphExists("someUser", DatabaseId.from("someDatabase"), "someGraph");

        assertTrue(graphExists);
    }

    @Test
    void shouldDetermineGraphDoesNotExist() {
        var service = mock(GraphStoreCatalogService.class);
        var facade = new GraphStoreCatalogBusinessFacade(
            mock(PreconditionsService.class),
            mock(GraphNameValidationService.class),
            service
        );

        when(service.graphExists("someUser", DatabaseId.from("someDatabase"), "someGraph")).thenReturn(false);
        boolean graphExists = facade.graphExists("someUser", DatabaseId.from("someDatabase"), "someGraph");

        assertFalse(graphExists);
    }

    @Test
    void shouldCheckPreconditions() {
        var preconditionsService = mock(PreconditionsService.class);
        var facade = new GraphStoreCatalogBusinessFacade(
            preconditionsService,
            null,
            null
        );

        doThrow(new IllegalStateException("call blocked because reasons")).when(preconditionsService)
            .checkPreconditions();
        assertThatThrownBy(
            () -> facade.graphExists("someUser", DatabaseId.from("someDatabase"), "someGraph")
        ).hasMessage("call blocked because reasons");
    }

    @Test
    void shouldValidateInputName() {
        var service = mock(GraphStoreCatalogService.class);
        var graphNameValidationService = mock(GraphNameValidationService.class);
        var facade = new GraphStoreCatalogBusinessFacade(
            mock(PreconditionsService.class),
            graphNameValidationService,
            service
        );

        doThrow(new IllegalStateException("'some blank graph name' is considered blank"))
            .when(graphNameValidationService)
            .ensureIsNotBlank("some blank graph name");
        assertThatThrownBy(
            () -> facade.graphExists("someUser", DatabaseId.from("someDatabase"), "some blank graph name")
        ).hasMessage("'some blank graph name' is considered blank");
    }
}
