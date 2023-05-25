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
package org.neo4j.gds.pregel;

import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.neo4j.gds.beta.pregel.PregelProcedureConfig;
import org.neo4j.gds.beta.pregel.annotation.GDSMode;
import org.neo4j.gds.pregel.generator.TypeNames;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationGeneratorTest {

    private static final String NL = System.lineSeparator();

    @Test
    void shouldGenerateType() {
        var configTypeName = TypeName.get(PregelProcedureConfig.class);
        var specificationGenerator = new SpecificationGenerator(new TypeNames("gds.test", "Foo", configTypeName));
        var specificationType = specificationGenerator.typeSpec(GDSMode.STATS, Optional.empty());

        assertThat(specificationType.toString()).isEqualTo("" +
            "public final class FooStatsSpecification implements org.neo4j.gds.executor.AlgorithmSpec<" +
            "gds.test.FooAlgorithm, " +
            "org.neo4j.gds.beta.pregel.PregelResult, " +
            "org.neo4j.gds.beta.pregel.PregelProcedureConfig, " +
            "java.util.stream.Stream<org.neo4j.gds.pregel.proc.PregelStatsResult>, " +
            "gds.test.FooAlgorithmFactory> {" +
            System.lineSeparator() +
            "}" +
            System.lineSeparator()
        );
    }

    @Test
    void shouldGenerateNameMethod() {
        var configTypeName = TypeName.get(PregelProcedureConfig.class);
        var specificationGenerator = new SpecificationGenerator(new TypeNames("gds.test", "Foo", configTypeName));
        assertThat(specificationGenerator.nameMethod().toString()).isEqualTo("" +
            "@java.lang.Override" + NL +
            "public java.lang.String name() {" + NL +
            "  return gds.test.FooAlgorithm.class.getSimpleName();" + NL +
            "}" + NL
        );
    }

    @Test
    void shouldGenerateAlgorithmFactoryMethod() {
        var configTypeName = TypeName.get(PregelProcedureConfig.class);
        var specificationGenerator = new SpecificationGenerator(new TypeNames("gds.test", "Foo", configTypeName));
        assertThat(specificationGenerator.algorithmFactoryMethod().toString()).isEqualTo("" +
            "@java.lang.Override" + NL +
            "public gds.test.FooAlgorithmFactory algorithmFactory(" + NL +
            "    org.neo4j.gds.executor.ExecutionContext executionContext) {" + NL +
            "  return new gds.test.FooAlgorithmFactory();" + NL +
            "}" + NL
        );
    }

    @Test
    void shouldGenerateNewConfigFunctionMethod() {
        var configTypeName = TypeName.get(PregelProcedureConfig.class);
        var specificationGenerator = new SpecificationGenerator(new TypeNames("gds.test", "Foo", configTypeName));
        assertThat(specificationGenerator.newConfigFunctionMethod().toString()).isEqualTo("" +
            "@java.lang.Override" + NL +
            "public org.neo4j.gds.executor.NewConfigFunction<org.neo4j.gds.beta.pregel.PregelProcedureConfig> newConfigFunction(" + NL +
            "    ) {" + NL +
            "  return (__, userInput) -> org.neo4j.gds.beta.pregel.PregelProcedureConfig.of(userInput);" + NL +
            "}" + NL
        );
    }

    @Test
    void shouldGenerateComputationResultConsumerMethod() {
        var configTypeName = TypeName.get(PregelProcedureConfig.class);
        var specificationGenerator = new SpecificationGenerator(new TypeNames("gds.test", "Foo", configTypeName));
        var computationResultConsumerMethod = specificationGenerator.computationResultConsumerMethod(GDSMode.STATS).toString();
        assertThat(computationResultConsumerMethod).isEqualTo("" +
            "@java.lang.Override" + NL +
            "public org.neo4j.gds.executor.ComputationResultConsumer<gds.test.FooAlgorithm, org.neo4j.gds.beta.pregel.PregelResult, org.neo4j.gds.beta.pregel.PregelProcedureConfig, java.util.stream.Stream<org.neo4j.gds.pregel.proc.PregelStatsResult>> computationResultConsumer(" + NL +
            "    ) {" + NL +
            "  return new org.neo4j.gds.pregel.proc.PregelStatsComputationResultConsumer<>();" + NL +
            "}" + NL
        );
    }

    @EnumSource(value = GDSMode.class)
    @ParameterizedTest
    void shouldGenerateDifferentlyPerMode(GDSMode mode) {
        var configTypeName = TypeName.get(PregelProcedureConfig.class);
        var specificationGenerator = new SpecificationGenerator(new TypeNames("gds.test", "Foo", configTypeName));
        var specificationType = specificationGenerator.typeSpec(mode, Optional.empty());
        assertThat(specificationType.toString())
            .contains("org.neo4j.gds.pregel.proc.Pregel" + mode.camelCase() + "Result");
        var computationResultConsumerMethod = specificationGenerator.computationResultConsumerMethod(mode);
        assertThat(computationResultConsumerMethod.toString())
            .contains("org.neo4j.gds.pregel.proc.Pregel" + mode.camelCase() + "ComputationResultConsumer");
    }
}
