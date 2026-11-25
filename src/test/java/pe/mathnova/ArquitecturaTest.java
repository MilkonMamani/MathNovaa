package pe.mathnova;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArquitecturaTest {

    private static JavaClasses clases;

    @BeforeAll
    static void importar() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("pe.mathnova");
    }

    @Test
    void dominioSinFrameworks() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "javafx..", "jakarta.persistence..", "org.hibernate..")
                .check(clases);
    }

    @Test
    void dominioNoMiraHaciaAfuera() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..presentation..", "..application..", "..infrastructure..")
                .check(clases);
    }
}
