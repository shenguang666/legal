package com.legal.knowledge.service;

import com.legal.enums.DocumentParseMethod;

public interface DocumentParser {

    DocumentParseMethod method();

    DocumentParseResult parse(DocumentParseRequest request);
}
