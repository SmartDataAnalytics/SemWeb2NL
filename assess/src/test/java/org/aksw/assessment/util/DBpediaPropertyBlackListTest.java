package org.aksw.assessment.util;

import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lorenz Buehmann
 *         created on 10/27/15
 */
public class DBpediaPropertyBlackListTest {

    DBpediaPropertyBlackList blackList;

    @Before
    public void setUp() throws Exception {
        blackList = new DBpediaPropertyBlackList();
    }

    @Test
    public void testContains() throws Exception {
        Assert.assertTrue(blackList.contains("http://dbpedia.org/ontology/abstract"));
        Assert.assertFalse(blackList.contains("http://dbpedia.org/ontology/birthPlace"));
    }

    @Test
    public void testContains1() throws Exception {
        Assert.assertTrue(blackList.contains(ResourceFactory.createResource("http://dbpedia.org/ontology/abstract")));
        Assert.assertFalse(blackList.contains(ResourceFactory.createResource("http://dbpedia.org/ontology/birthPlace")));
    }
}