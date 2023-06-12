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
package org.neo4j.gds.procedure.facade;

import org.neo4j.function.ThrowingFunction;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.catalog.DatabaseIdService;
import org.neo4j.gds.catalog.UserLogServices;
import org.neo4j.gds.catalog.UsernameService;
import org.neo4j.gds.core.utils.warnings.UserLogRegistryFactory;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.procedure.Context;

/**
 * @deprecated Needed until we strangle the last context-injected usages of {@link org.neo4j.gds.core.utils.warnings.UserLogRegistryFactory}
 */
@Deprecated
public class UserLogRegistryFactoryProvider implements ThrowingFunction<Context, UserLogRegistryFactory, ProcedureException> {
    private final DatabaseIdService databaseIdService;
    private final UsernameService usernameService;
    private final UserLogServices userLogServices;

    public UserLogRegistryFactoryProvider(
        DatabaseIdService databaseIdService,
        UsernameService usernameService,
        UserLogServices userLogServices
    ) {
        this.databaseIdService = databaseIdService;
        this.usernameService = usernameService;
        this.userLogServices = userLogServices;
    }

    @Override
    public UserLogRegistryFactory apply(Context context) {
        DatabaseId databaseId = databaseIdService.getDatabaseId(context.graphDatabaseAPI());
        String username = usernameService.getUsername(context.securityContext());

        return userLogServices.getUserLogRegistryFactory(databaseId, username);
    }
}
