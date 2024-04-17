package com.antalex.db.service.impl.wrapers;

import com.antalex.db.service.api.DataWrapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Optional;

public class JSonWrapper implements DataWrapper {
    private ObjectNode root;
    private ObjectMapper objectMapper;

    public JSonWrapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(String content) throws IOException {
        this.root = content == null ?
                objectMapper.createObjectNode() :
                (ObjectNode) objectMapper.readTree(content);
    }

    @Override
    public void put(String attribute, Object o) {
        this.root.putPOJO(attribute, o);
    }

    @Override
    public <T> T get(String attribute, Class<T> clazz) throws JsonProcessingException {
        JsonNode nodeAttribute = this.root.get(attribute);
        return objectMapper.treeToValue(nodeAttribute, clazz);
    }

    @Override
    public String getContent() {
        return Optional.ofNullable(root).map(ObjectNode::toString).orElse(null);
    }
}
