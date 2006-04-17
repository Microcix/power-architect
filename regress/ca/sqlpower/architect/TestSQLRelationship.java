package regress.ca.sqlpower.architect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLObject;
import ca.sqlpower.architect.SQLObjectEvent;
import ca.sqlpower.architect.SQLObjectListener;
import ca.sqlpower.architect.SQLRelationship;
import ca.sqlpower.architect.SQLTable;

public class TestSQLRelationship extends SQLTestCase {

	private SQLTable parentTable;
	private SQLTable childTable1;
	private SQLTable childTable2;
	private SQLRelationship rel1;
	private SQLRelationship rel2;
	private SQLDatabase database;
	
	public TestSQLRelationship(String name) throws Exception {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.out.println("-------------Starting New Test "+getName()+" -------------");
		
		database = new SQLDatabase();
		parentTable = new SQLTable(database, "parent", null, "TABLE", true);
		parentTable.addColumn(new SQLColumn(parentTable, "pkcol_1", Types.INTEGER, 10, 0));
		parentTable.addColumn(new SQLColumn(parentTable, "pkcol_2", Types.INTEGER, 10, 0));
		parentTable.addColumn(new SQLColumn(parentTable, "attribute_1", Types.INTEGER, 10, 0));
		database.addChild(parentTable);
		childTable1 = new SQLTable(database, "child_1", null, "TABLE", true);
		childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_1", Types.INTEGER, 10, 0));
		childTable1.addColumn(new SQLColumn(childTable1, "child_pkcol_2", Types.INTEGER, 10, 0));
		childTable1.addColumn(new SQLColumn(childTable1, "child_attribute", Types.INTEGER, 10, 0));
		database.addChild(childTable1);
		
		childTable2 = new SQLTable(database, "child_2", null, "TABLE", true);
		childTable2.addColumn(new SQLColumn(childTable2, "child2_pkcol_1", Types.INTEGER, 10, 0));
		childTable2.addColumn(new SQLColumn(childTable2, "child2_pkcol_2", Types.INTEGER, 10, 0));
		childTable2.addColumn(new SQLColumn(childTable2, "child2_attribute", Types.INTEGER, 10, 0));
		database.addChild(childTable2);
		
		rel1 = new SQLRelationship();
		rel1.setIdentifying(true);
		rel1.attachRelationship(parentTable,childTable1,false);
		rel1.setName("rel1");
		rel1.addMapping(parentTable.getColumn(0), childTable1.getColumn(0));
		rel1.addMapping(parentTable.getColumn(1), childTable1.getColumn(1));
	
		rel2 = new SQLRelationship();
		rel2.setName("rel2");
		rel2.attachRelationship(parentTable,childTable2,true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		assertFalse("Parent table was left in secondary mode!", parentTable.isSecondaryChangeMode());
		assertFalse("Child table 1 was left in secondary mode!", childTable1.isSecondaryChangeMode());
		assertFalse("Child table 2 was left in secondary mode!", childTable2.isSecondaryChangeMode());
		assertFalse("Rel 1 was left in secondary mode!", rel1.isSecondaryChangeMode());
		assertFalse("Rel 2 was left in secondary mode!", rel2.isSecondaryChangeMode());
		super.tearDown();
	}
	
	/**
	 * Returns one of the relationships that setUp makes.
	 * Right now, it's rel1.
	 */
	@Override
	protected SQLObject getSQLObjectUnderTest() {
		return rel1;
	}
	
	public void testSetPhysicalName() {
		CountingSQLObjectListener l = new CountingSQLObjectListener();
		rel1.addSQLObjectListener(l);
		
		// ensure all event counts start with 0
		assertEquals(0, l.getInsertedCount());
		assertEquals(0, l.getRemovedCount());
		assertEquals(0, l.getChangedCount());
		assertEquals(0, l.getStructureChangedCount());
		
		rel1.setPhysicalName("test_new_name");
		
		// ensure only dbObjectChanged was called (we omit this check for the remainder of the tests)
		assertEquals(0, l.getInsertedCount());
		assertEquals(0, l.getRemovedCount());
		assertEquals(1, l.getChangedCount());
		assertEquals(0, l.getStructureChangedCount());
		
		assertEquals("new name didn't stick", "test_new_name", rel1.getPhysicalName());
		
		rel1.setPhysicalName("test_new_name");
		assertEquals(1, l.getChangedCount());

		rel1.setPhysicalName("test_actual_new_name");
		assertEquals(2, l.getChangedCount());

		rel1.setPhysicalName(null);
		assertEquals(3, l.getChangedCount());
		assertEquals("new name didn't go back to logical name", rel1.getName(), rel1.getPhysicalName());

		rel1.setPhysicalName(null);
		assertEquals(3, l.getChangedCount());

		// double-check that none of the other event types got fired
		assertEquals(0, l.getInsertedCount());
		assertEquals(0, l.getRemovedCount());
		assertEquals(0, l.getStructureChangedCount());
	}

	public void testReadFromDB() throws Exception {
		Connection con = db.getConnection();
		Statement stmt = null;
		String lastSQL = null;
		try {
			stmt = con.createStatement();

			try {
				stmt.executeUpdate("DROP TABLE relationship_test_child");
			} catch (SQLException ex) {
				System.out.println("Ignoring SQL Exception; assume relationship_test_child didn't exist.");
				System.out.println(ex.getMessage());
			}

			try {
				stmt.executeUpdate("DROP TABLE relationship_test_parent");
			} catch (SQLException ex) {
				System.out.println("Ignoring SQL Exception; assume relationship_test_parent didn't exist.");
				System.out.println(ex.getMessage());
			}

			lastSQL = "CREATE TABLE relationship_test_parent (\n" +
					  " pkcol_1 integer not null,\n" +
					  " pkcol_2 integer not null,\n" +
					  " attribute_1 integer not null)";
			stmt.executeUpdate(lastSQL);

			lastSQL = "CREATE TABLE relationship_test_child (\n" +
			          " parent_pkcol_1 integer not null,\n" +
			          " parent_pkcol_2 integer not null,\n" +
			          " child_attribute_1 integer not null)";
			stmt.executeUpdate(lastSQL);
			
			lastSQL = "ALTER TABLE relationship_test_parent\n" +
			          " ADD CONSTRAINT relationship_test_pk\n" +
			          " PRIMARY KEY (pkcol_1 , pkcol_2)";
			stmt.executeUpdate(lastSQL);
			
			lastSQL = "ALTER TABLE relationship_test_child\n" +
			          " ADD CONSTRAINT relationship_test_fk\n" +
			          " FOREIGN KEY (parent_pkcol_1, parent_pkcol_2)\n" +
			          " REFERENCES relationship_test_parent (pkcol_1 , pkcol_2)";
			stmt.executeUpdate(lastSQL);
			
		} catch (SQLException ex) {
			System.out.println("SQL Statement Failed:\n"+lastSQL+"\nStack trace of SQLException follows:");
			ex.printStackTrace();
			fail("SQL statement failed. See system console for details.");
		} finally {
			if (stmt != null) stmt.close();
		}
		
		SQLTable parent = db.getTableByName("relationship_test_parent");
		SQLTable child = db.getTableByName("relationship_test_child");
		
		if (parent == null) {
			parent = db.getTableByName("relationship_test_parent".toUpperCase());
		}
		SQLRelationship rel = (SQLRelationship) parent.getExportedKeys().get(0);
		
		assertEquals("relationship_test_fk", rel.getName().toLowerCase());
		assertSame(parent, rel.getPkTable());
		assertSame(child, rel.getFkTable());
		assertEquals((SQLRelationship.ZERO | SQLRelationship.ONE | SQLRelationship.MANY), rel.getFkCardinality());
		assertEquals(SQLRelationship.ONE, rel.getPkCardinality());
	}

	public void testAllowsChildren() {
		assertTrue(rel1.allowsChildren());
	}

	public void testSQLRelationship() throws ArchitectException {
		SQLRelationship rel = new SQLRelationship();
		assertNotNull(rel.getChildren());
		assertNotNull(rel.getSQLObjectListeners());
	}

	public void testGetMappingByPkCol() throws ArchitectException {
		SQLColumn col = parentTable.getColumnByName("pkcol_1");
		SQLRelationship.ColumnMapping m = rel1.getMappingByPkCol(col);
		assertEquals("pkcol_1", m.getPkColumn().getName());
		assertEquals("child_pkcol_1", m.getFkColumn().getName());

		// check another column (in case it always returns the first mapping or something)
		col = parentTable.getColumnByName("pkcol_2");
		m = rel1.getMappingByPkCol(col);
		assertEquals("pkcol_2", m.getPkColumn().getName());
		assertEquals("child_pkcol_2", m.getFkColumn().getName());
	}
	
	public void testGetNonExistentMappingByPkCol() throws ArchitectException {
		// check a column that's in the PK table but not in the mapping
		SQLColumn col = parentTable.getColumnByName("attribute_1");
		SQLRelationship.ColumnMapping m = rel1.getMappingByPkCol(col);
		assertNull(m);
	}

	/** This was a real regression */
	public void testDeletePkColRemovesFkCol() throws ArchitectException {
		SQLColumn pkcol = parentTable.getColumnByName("pkcol_1");
		assertNotNull("Child col should exist to start", childTable1.getColumnByName("child_pkcol_1"));
		parentTable.removeColumn(pkcol);
		assertNull("Child col should have been removed", childTable1.getColumnByName("child_pkcol_1"));
	}
	
	/**
	 * testing that a column gets hijacked and promoted to the primary key
	 * when the corrisponding pk column is added into the primary key 
	 * 
	 * @throws ArchitectException
	 */
	public void testHijackedColumnGoesToPK() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "hijack", Types.INTEGER, 10, 0);
		SQLColumn fkcol = new SQLColumn(childTable1, "hijack", Types.INTEGER, 10, 0);
		SQLRelationship rel = parentTable.getExportedKeys().get(0);
		childTable1.addColumn(0, fkcol);
		parentTable.addColumn(0, pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		assertNotNull("parent column didn't to go PK", pkcol.getPrimaryKeySeq());
		assertTrue("column didn't get hijacked", rel.containsFkColumn(fkcol));
		
		// this is the point of the test
		assertNotNull("column didn't go to primary key", fkcol.getPrimaryKeySeq());
	}
	
	/**
	 * testing that a column gets hijacked and promoted to the primary key
	 * when the corrisponding pk column is moved into the primary key from further
	 * down in its column list. 
	 * 
	 * @throws ArchitectException
	 */
	public void testHijackedColumnGoesToPK2() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "hijack", Types.INTEGER, 10, 0);
		SQLColumn fkcol = new SQLColumn(childTable1, "hijack", Types.INTEGER, 10, 0);
		SQLRelationship rel = parentTable.getExportedKeys().get(0);
		childTable1.addColumn( fkcol);
		parentTable.addColumn( pkcol);
		assertNull("pkcol already in the primary key",pkcol.getPrimaryKeySeq());
		pkcol.setPrimaryKeySeq(0);
		
		assertNotNull("parent column didn't to go PK", pkcol.getPrimaryKeySeq());
		assertTrue("column didn't get hijacked", rel.containsFkColumn(fkcol));
		
		// this is the point of the test
		assertNotNull("column didn't go to primary key", fkcol.getPrimaryKeySeq());
	}
	
	public void testFKColManagerRemovesImportedKey() throws ArchitectException {
		assertTrue("Parent table should export rel1",parentTable.getExportedKeys().contains(rel1));
		assertTrue("childTable1 should import rel1",childTable1.getImportedKeys().contains(rel1));
		assertEquals("Child's imported count is whacked out", 1, childTable1.getImportedKeys().size());
		
		assertNotNull("Missing imported key", childTable1.getColumnByName("child_pkcol_1"));
		assertNotNull("Missing imported key", childTable1.getColumnByName("child_pkcol_2"));
		int oldChildColCount = childTable1.getColumns().size();
		
		parentTable.removeExportedKey(rel1);

		assertFalse("Parent table should not export rel1 any more", parentTable.getExportedKeys().contains(rel1));
		assertFalse("childTable1 should not import rel1 any more", childTable1.getImportedKeys().contains(rel1));
				
		// the following tests depend on FKColumnManager behaviour, not UndoManager
		assertEquals("Relationship still attached to child", 0, childTable1.getImportedKeys().size());
		assertNull("Orphaned imported key", childTable1.getColumnByName("child_pkcol_1"));
		assertNull("Orphaned imported key", childTable1.getColumnByName("child_pkcol_2"));
		assertEquals("Child column list should have shrunk by 2", oldChildColCount - 2, childTable1.getColumns().size());
		assertNotNull("Missing exported key", parentTable.getColumnByName("pkcol_1"));
		assertNotNull("Missing exported key", parentTable.getColumnByName("pkcol_2"));
	}
	
	public void testRemovedRelationshipsDontInterfere() throws ArchitectException {
		testFKColManagerRemovesImportedKey();
		
		int oldChildColCount = childTable1.getColumns().size();
		
		SQLColumn pk3 = new SQLColumn(parentTable, "pk3", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pk3);
		pk3.setPrimaryKeySeq(0);
		
		assertEquals("Child table got new col!?!", oldChildColCount, childTable1.getColumns().size());
	}
	
	public void testRemoveChildTable() throws ArchitectException {
		
		assertEquals(3,database.getChildCount());
		assertEquals(2,parentTable.getExportedKeys().size());
		
		database.removeChild(childTable1);
		assertEquals(2,database.getChildCount());
		assertEquals(1,parentTable.getExportedKeys().size());
		
		assertNull("Child table not removed from the database",
				database.getTableByName(childTable1.getName()));
		assertFalse("Parent still contains a reference to a deleted table", 
				parentTable.getExportedKeys().contains(rel1));
		
		database.removeChild(childTable2);
		
		assertNull("Child table 2 not removed from the database",
				database.getTableByName(childTable2.getName()));
		assertFalse("Parent still contains a reference to a deleted table", 
				parentTable.getExportedKeys().contains(rel2));
		
		assertEquals(1,database.getChildCount());
		assertEquals(0,parentTable.getExportedKeys().size());
	}
	
	public void testRemoveParentTable() throws ArchitectException {
		database.removeChild(parentTable);
		assertNull("Child table not removed from the database",database.getTableByName(parentTable.getName()));
		assertFalse("Parent still contains a reference to a deleted table", 
				parentTable.getExportedKeys().contains(rel1));
	}
	
	public void testPKColNameChangeGoesToFKColWhenNamesWereSame() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		pkcol.setName("new name");
		
		assertEquals("fkcol's name didn't update", "new name", fkcol.getName());
	}

	public void testPKColNameChangeDoesntGoToFKColWhenNamesWereDifferent() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		fkcol.setName("custom fk col name");
		pkcol.setName("new name");
		
		assertEquals("fkcol's name didn't update", "custom fk col name", fkcol.getName());
	}

	public void testPKColNameChangeGoesToFKIsSecondaryEvent() throws ArchitectException{				
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		MySQLObjectListener l = new MySQLObjectListener();
		ArchitectUtils.listenToHierarchy(l, database);
		
		pkcol.setName("new name");
		
		// Expect one primary event for the name change in the parent table
		assertEquals("too many primary events!", 1, l.getPrimaryCount());
		
		// expect secondary events for the related columns in child_table_1 and child_table_2
		assertEquals("not enough secondary events!", 2, l.getSecondaryCount());
		
		// make sure columns aren't stuck in secondary mode
		assertFalse(parentTable.getColumnByName("new name").isSecondaryChangeMode());
		assertFalse(childTable1.getColumnByName("new name").isSecondaryChangeMode());
		assertFalse(childTable2.getColumnByName("new name").isSecondaryChangeMode());
	}
	
	public void testPKColTypeChangeGoesToFKCol() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		pkcol.setType(Types.BINARY);
		
		assertEquals("fkcol's type didn't update", Types.BINARY, fkcol.getType());
	}

	public void testPKColPrecisionChangeGoesToFKCol() throws ArchitectException {
		SQLColumn pkcol = new SQLColumn(parentTable, "old name", Types.VARCHAR, 10, 0);
		parentTable.addColumn(pkcol);
		pkcol.setPrimaryKeySeq(0);
		
		SQLColumn fkcol = childTable1.getColumnByName("old name");
		
		pkcol.setPrecision(20);
		
		assertEquals("fkcol's precision didn't update", 20, fkcol.getPrecision());
	}

	/** This is something the undo manager will attempt when you undo deleting a relationship */
	public void testReconnectOldRelationshipWithCustomMapping() throws ArchitectException {
		List<SQLColumn> origParentCols = new ArrayList<SQLColumn>(parentTable.getColumns()); 
		List<SQLColumn> origChild1Cols = new ArrayList<SQLColumn>(childTable1.getColumns());
		
		parentTable.removeExportedKey(rel1);
		rel1.attachRelationship(parentTable, childTable1, false);
		
		assertEquals("Exported key columns disappeared", origParentCols, parentTable.getColumns());
		assertEquals("Imported key columns didn't get put back", origChild1Cols, childTable1.getColumns());
		assertEquals("There are multiple copies of this relationship in the parent's export keys folder",2,parentTable.getExportedKeys().size());
		assertEquals("There are multiple copies of this relationship in the child's import keys folder",1,childTable1.getImportedKeys().size());
	}
	
	/** This is something the undo manager will attempt when you undo deleting a relationship */
	public void testReconnectOldRelationshipWithAutoMapping() throws ArchitectException {
		SQLTable myParent = new SQLTable(db, true);
		SQLColumn col;
		myParent.addColumn(col = new SQLColumn(myParent, "pkcol1", Types.VARCHAR, 10, 0));
		col.setPrimaryKeySeq(0);
		myParent.addColumn(col = new SQLColumn(myParent, "pkcol2", Types.VARCHAR, 10, 0));
		col.setPrimaryKeySeq(0);
		
		SQLTable myChild = new SQLTable(db, true);
		
		SQLRelationship myRel = new SQLRelationship();
		myRel.attachRelationship(myParent, myChild, true);
		List<SQLColumn> origParentCols = new ArrayList<SQLColumn>(myParent.getColumns()); 
		List<SQLColumn> origChildCols = new ArrayList<SQLColumn>(myChild.getColumns());

		// the next two lines are what the business model sees from undo/redo
		myParent.removeExportedKey(myRel);
		myRel.attachRelationship(myParent, myChild, false);
		
		assertEquals("Exported key columns disappeared", origParentCols, myParent.getColumns());
		assertEquals("Imported key columns didn't get put back", origChildCols, myChild.getColumns());
		assertEquals("There are multiple copies of this relationship in the parent's export keys folder",1,myParent.getExportedKeys().size());
		assertEquals("There are multiple copies of this relationship in the child's import keys folder",1,myChild.getImportedKeys().size());
	}

	
	private class MySQLObjectListener implements SQLObjectListener {

		private int primaryCount = 0;
		private int secondaryCount = 0;
		
		public void dbChildrenInserted(SQLObjectEvent e) {
			if (e.isSecondary()) {
				secondaryCount++;
			} else {
				primaryCount++;
				System.out.printf("Primary dbChildredInserted: parent=%s, children=%s\n", e.getSource(), Arrays.asList(e.getChildren()));
			}
		}

		public void dbChildrenRemoved(SQLObjectEvent e) {
			if (e.isSecondary()) {
				secondaryCount++;
			} else {
				primaryCount++;
				System.out.printf("Primary dbChildredRemoved: parent=%s, children=%s\n", e.getSource(), Arrays.asList(e.getChildren()));
				new Exception().printStackTrace(System.out);
			}
		}

		public void dbObjectChanged(SQLObjectEvent e) {
			if (e.isSecondary()) {
				secondaryCount++;
			} else {
				primaryCount++;
				System.out.printf("Primary dbObjectChanged: source=%s (parent %s), prop=%s, old=%s, new=%s\n",
						e.getSource(), e.getSQLSource().getParent(), e.getPropertyName(), e.getOldValue(), e.getNewValue());
			}
		}

		public void dbStructureChanged(SQLObjectEvent e) {
			if (e.isSecondary()) {
				secondaryCount++;
			} else {
				primaryCount++;
				System.out.printf("Primary dbStructureChanged: parent=%s, children=%s\n", e.getSource(), e.getChildren());
			}
		}

		public int getPrimaryCount() {
			return primaryCount;
		}

		public int getSecondaryCount() {
			return secondaryCount;
		}
	}
	
	public void testAutoDeleteFKIsSecondary() throws ArchitectException {
		
		
		MySQLObjectListener myListener = new MySQLObjectListener();
		ArchitectUtils.listenToHierarchy(myListener, childTable1);
		
		parentTable.removeExportedKey(rel1);
		
		assertEquals("Got primary events (this is bad)", 0, myListener.getPrimaryCount());
		assertTrue("Didn't get any secondary events (this is bad)", myListener.getSecondaryCount() > 0);
	}
	
	public void testAttachRelationshipIsSecondary() throws ArchitectException {
		MySQLObjectListener myListener = new MySQLObjectListener();
		parentTable.removeExportedKey(rel1);
		
		ArchitectUtils.listenToHierarchy(myListener, childTable1);
		rel1.attachRelationship(parentTable,childTable1,false);
		assertEquals("Got primary events (this is bad)", 0, myListener.getPrimaryCount());
		assertTrue("Didn't get any secondary events (this is bad)", myListener.getSecondaryCount() > 0);
		
	}
	
	public void testMovingPKColOutOfPK() throws ArchitectException {
		SQLColumn col = parentTable.getColumnByName("pkcol_1");
		
		col.setPrimaryKeySeq(null);
		assertTrue("pkcol_1 dropped from the parent table", parentTable.getColumns().contains(col));
	}
	public void testMovingPKColOutOfPKByColIndex() throws ArchitectException {
		SQLColumn col = parentTable.getColumnByName("pkcol_2");
		int index = parentTable.getColumnIndex(col);
		parentTable.changeColumnIndex(index,1,false);
		assertTrue("pkcol_1 dropped from the parent table", parentTable.getColumns().contains(col));
	}
}
