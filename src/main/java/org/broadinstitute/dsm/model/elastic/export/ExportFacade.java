package org.broadinstitute.dsm.model.elastic.export;

public class ExportFacade {

    Exportable exportable;
    Generator generator;
    Parser parser;

    public ExportFacade(Exportable exportable, Generator generator, Parser parser) {
        this.exportable = exportable;
        this.generator = generator;
        this.parser = parser;
    }

    
}
