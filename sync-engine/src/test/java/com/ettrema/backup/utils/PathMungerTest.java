package com.ettrema.backup.utils;

import com.ettrema.backup.config.DavRepo;
import com.ettrema.backup.config.Root;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author HP
 */
public class PathMungerTest extends TestCase {
	
	PathMunger pathMunger;
	
	public PathMungerTest(String testName) {
		super(testName);
	}
	
	@Override
	protected void setUp() throws Exception {
		pathMunger = new PathMunger();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testfindFileFromUrl_Windows() {
		List<Root> roots = new ArrayList<Root>();
		Root root1 = new Root("\\files\\Docs", "/Documents"); // assume windows path
		roots.add(root1);
		String filePath = pathMunger.findFileFromUrl(roots, "http://www.somewhere.com/users/a1/Documents/abc.txt", "\\");
		System.out.println("filePath: " + filePath);
		assertEquals("\\files\\Docs\\abc.txt", filePath);
	}
	
	public void testfindFileFromUrl_Nix() {
		List<Root> roots = new ArrayList<Root>();
		DavRepo repo = new DavRepo();
		Root root1 = new Root("/files/Docs", "/Documents"); // assume windows path
		roots.add(root1);
		String filePath = pathMunger.findFileFromUrl(roots, "http://www.somewhere.com/users/a1/Documents/ab%20c.txt", "/");
		assertEquals("/files/Docs/ab c.txt", filePath);
	}	
	
	public void testRootUrl() {
		List<Root> roots = new ArrayList<Root>();
		Root root1 = new Root("", "/Documents"); // assume windows path
		roots.add(root1);
		String filePath = pathMunger.findFileFromUrl(roots, "http://www.somewhere.com/users/a1/Documents", "/");
		assertEquals("/", filePath);
	}	
}
