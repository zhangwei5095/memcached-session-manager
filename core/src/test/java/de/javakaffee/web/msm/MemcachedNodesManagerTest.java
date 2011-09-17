/*
 * Copyright 2011 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.javakaffee.web.msm;

import static de.javakaffee.web.msm.MemcachedNodesManager.createFor;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import de.javakaffee.web.msm.MemcachedNodesManager.MemcachedClientCallback;

/**
 * Test for {@link MemcachedNodesManager}.
 * 
 * @author @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class MemcachedNodesManagerTest {
	
	private MemcachedClientCallback _mcc;

	@BeforeMethod
	public void beforeClass() {
		_mcc = mock(MemcachedClientCallback.class);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testParseWithNullShouldThrowException() {
		createFor(null, null, _mcc);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testParseWithEmptyStringShouldThrowException() {
		createFor("", null, _mcc);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testSingleSimpleNodeAndFailoverNodeShouldThrowException() {
		createFor("localhost:11211", "n1", _mcc);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testSingleNodeAndFailoverNodeShouldThrowException() {
		createFor("n1:localhost:11211", "n1", _mcc);
	}
	
	@DataProvider
	public static Object[][] nodesAndExpectedCountDataProvider() {
		return new Object[][] {
				{ "localhost:11211", 1 },
                { "localhost:11211/default", 1},
                { "n1:localhost:11211", 1 },
				{ "n1:localhost:11211,n2:localhost:11212", 2 },
				{ "n1:localhost:11211 n2:localhost:11212", 2 }
		};
	}
	
	@Test( dataProvider = "nodesAndExpectedCountDataProvider" )
	public void testCountNodes( final String memcachedNodes, final int expectedCount ) {
		final MemcachedNodesManager result = createFor( memcachedNodes, null, _mcc );
		assertNotNull(result);
		assertEquals(result.getCountNodes(),  expectedCount);
	}
	
	@DataProvider
	public static Object[][] nodesAndPrimaryNodesDataProvider() {
		return new Object[][] {
				{ "localhost:11211", null, new NodeIdList() },
				{ "n1:localhost:11211", null, new NodeIdList("n1") },
				{ "n1:localhost:11211,n2:localhost:11212", "n1", new NodeIdList("n2") },
				{ "n1:localhost:11211,n2:localhost:11212,n3:localhost:11213", "n1", new NodeIdList("n2", "n3") },
				{ "n1:localhost:11211,n2:localhost:11212,n3:localhost:11213", "n1,n2", new NodeIdList("n3") }
		};
	}

	@Test( dataProvider = "nodesAndPrimaryNodesDataProvider" )
	public void testPrimaryNodes(final String memcachedNodes, final String failoverNodes, final NodeIdList expectedPrimaryNodeIds) {
		final MemcachedNodesManager result = createFor( memcachedNodes, failoverNodes, _mcc );
		assertNotNull(result);
		assertEquals(result.getPrimaryNodeIds(), expectedPrimaryNodeIds);	
	}
	
	@DataProvider
	public static Object[][] nodesAndFailoverNodesDataProvider() {
		return new Object[][] {
				{ "localhost:11211", null, Collections.emptyList() },
				{ "localhost:11211", "", Collections.emptyList() },
				{ "n1:localhost:11211", null, Collections.emptyList() },
				{ "n1:localhost:11211,n2:localhost:11212", "n1", Arrays.asList("n1") },
				{ "n1:localhost:11211,n2:localhost:11212,n3:localhost:11213", "n1,n2", Arrays.asList("n1", "n2") },
				{ "n1:localhost:11211,n2:localhost:11212,n3:localhost:11213", "n1 n2", Arrays.asList("n1", "n2") }
		};
	}

	@Test( dataProvider = "nodesAndFailoverNodesDataProvider" )
	public void testFailoverNodes(final String memcachedNodes, final String failoverNodes, final List<String> expectedFailoverNodeIds) {
		final MemcachedNodesManager result = createFor( memcachedNodes, failoverNodes, _mcc );
		assertNotNull(result);
		assertEquals(result.getFailoverNodeIds(), expectedFailoverNodeIds);	
	}
	
	@DataProvider
	public static Object[][] nodesAndExpectedEncodedInSessionIdDataProvider() {
		return new Object[][] {
				{ "localhost:11211", null, false },
				{ "n1:localhost:11211", null, true },
				{ "n1:localhost:11211,n2:localhost:11212", "n1", true }
		};
	}
	
	@Test( dataProvider = "nodesAndExpectedEncodedInSessionIdDataProvider" )
	public void testIsEncodeNodeIdInSessionId( final String memcachedNodes, final String failoverNodes, final boolean expectedIsEncodeNodeIdInSessionId ) {
		final MemcachedNodesManager result = createFor( memcachedNodes, null, _mcc );
		assertNotNull(result);
		assertEquals(result.isEncodeNodeIdInSessionId(), expectedIsEncodeNodeIdInSessionId);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testGetNodeIdShouldThrowExceptionForNullArgument() {
		final MemcachedNodesManager result = createFor( "n1:localhost:11211", null, _mcc );
		result.getNodeId(null);
	}
	
	@DataProvider
	public static Object[][] testGetNodeIdDataProvider() {
		return new Object[][] {
				{ "n1:localhost:11211", null, new InetSocketAddress("localhost", 11211), "n1" },
				{ "n1:localhost:11211,n2:localhost:11212", null, new InetSocketAddress("localhost", 11212), "n2" },
				{ "n1:localhost:11211,n2:localhost:11212", "n1", new InetSocketAddress("localhost", 11211), "n1" }
		};
	}

	@Test( dataProvider = "testGetNodeIdDataProvider" )
	public void testGetNodeId(final String memcachedNodes, final String failoverNodes, final InetSocketAddress socketAddress, final String expectedNodeId) {
		final MemcachedNodesManager result = createFor( memcachedNodes, failoverNodes, _mcc );
		assertEquals(result.getNodeId(socketAddress), expectedNodeId);	
	}
	
	/**
	 * Test for {@link MemcachedNodesManager#getNextPrimaryNodeId(String)}.
	 * @see NodeIdList#getNextNodeId(String)
	 * @see NodeIdListTest#testGetNextNodeId()
	 */
	@Test
	public void testGetNextPrimaryNodeId() {
		assertNull(createFor( "n1:localhost:11211", null, _mcc ).getNextPrimaryNodeId("n1"));
		assertEquals(createFor( "n1:localhost:11211,n2:localhost:11212", null, _mcc ).getNextPrimaryNodeId("n1"), "n2");
	}
	
	@DataProvider
	public static Object[][] testgGetAllMemcachedAddressesDataProvider() {
		return new Object[][] {
				{ "n1:localhost:11211", null, asList(new InetSocketAddress("localhost", 11211)) },
				{ "n1:localhost:11211,n2:localhost:11212", null, asList(new InetSocketAddress("localhost", 11211), new InetSocketAddress("localhost", 11212)) },
				{ "n1:localhost:11211,n2:localhost:11212", "n1", asList(new InetSocketAddress("localhost", 11211), new InetSocketAddress("localhost", 11212)) }
		};
	}
	
	@Test( dataProvider = "testgGetAllMemcachedAddressesDataProvider" )
	public void testgGetAllMemcachedAddresses(final String memcachedNodes, final String failoverNodes, final Collection<InetSocketAddress> expectedSocketAddresses) {
		final MemcachedNodesManager result = createFor( memcachedNodes, failoverNodes, _mcc );
		assertEquals(result.getAllMemcachedAddresses(), expectedSocketAddresses);
	}
	
	@Test
	public void testGetSessionIdFormat() {
		final SessionIdFormat sessionIdFormat = createFor( "n1:localhost:11211", null, _mcc ).getSessionIdFormat();
		assertNotNull(sessionIdFormat);
	}
	
	@Test
	public void testCreateSessionIdShouldOnlyAddNodeIdIfPresent() {
		assertEquals(createFor( "n1:localhost:11211", null, _mcc ).createSessionId("foo"), "foo-n1" );
		assertEquals(createFor( "localhost:11211", null, _mcc ).createSessionId("foo"), "foo" );
	}
	
	@Test
	public void testSetNodeAvailable() {
		final MemcachedNodesManager cut = createFor( "n1:localhost:11211,n2:localhost:11212", null, _mcc );
		assertTrue(cut.isNodeAvailable("n1"));
		assertTrue(cut.isNodeAvailable("n2"));
		
		cut.setNodeAvailable("n1", false);

		assertFalse(cut.isNodeAvailable("n1"));
		assertTrue(cut.isNodeAvailable("n2"));
	}

}