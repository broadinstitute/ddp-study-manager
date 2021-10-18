package org.broadinstitute.dsm.model.elastic.export;

import java.util.Map;

public class UpdateRequestPayload {

    private String index;
    private String type;
    private String id;
    private Map<String, Object> doc;
    private boolean docAsUpsert;
    private int retryOnConflict;

    private UpdateRequestPayload(Builder builder) {
        this.index = builder.index;
        this.type = builder.type;
        this.id = builder.id;
        this.doc = builder.doc;
        this.docAsUpsert = builder.docAsUpsert;
        this.retryOnConflict = builder.retryOnConflict;
    }

    private static class Builder {
        public String index;
        public String type;
        public String id;
        public Map<String, Object> doc;
        public boolean docAsUpsert;
        public int retryOnConflict;

        public Builder(String index, String type, String id, Map<String, Object> doc) {
            this.index = index;
            this.type = type;
            this.id = id;
            this.doc = doc;
        }

        public Builder withIndex(String index) {
            this.index = index;
            return this;
        }
        
        public Builder withType(String type) {
            this.type = type;
            return this;
        }
        
        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withDoc(Map<String, Object> docToUpsert) {
            this.doc = docToUpsert;
            return this;
        }

        public Builder withDocAsUpsert(boolean docAsUpsert) {
            this.docAsUpsert = docAsUpsert;
            return this;
        }

        public Builder withRetryOnConflict(int retryOnConflict) {
            this.retryOnConflict = retryOnConflict;
            return this;
        }

        public UpdateRequestPayload build() {
            return new UpdateRequestPayload(this);
        }


    }

}
