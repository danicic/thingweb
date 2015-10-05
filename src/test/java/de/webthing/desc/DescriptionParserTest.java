package de.webthing.desc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;

public class DescriptionParserTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testFromURL() {
	// TODO
//	fail("Not yet implemented");
    }

    @Test
    public void testFromFile() {
	String happyPath = "jsonld" + File.separator + "led.jsonld";
	// (1) the document is not "compact" as per JSON-LD API spec ('td:' prefix already defined in the context)
	// (2) the property 'label' does not appear in the context (should be dropped)
	// (3) 'encodings' has only one member and is not defined as a list (should be transformed by compaction)
	String altPath = "jsonld" + File.separator + "led_1.jsonld";
	// JSON syntax error (missing comma)
	// note : the JSON-LD API spec is very permissive. Hard to get a JsonLdError...
	String erroneous = "jsonld" + File.separator + "led_2.jsonld";
	
	try {
	    DescriptionParser.fromFile(happyPath);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail();
	}
	
	try {
	    DescriptionParser.fromFile(altPath);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail();
	}
	
	try {
	    DescriptionParser.fromFile(erroneous);
	    fail();
	} catch (IOException e) {
	    if (e instanceof JsonParseException) {
		// as expected
	    } else {
		e.printStackTrace();
		fail();
	    }
	}
    }

}