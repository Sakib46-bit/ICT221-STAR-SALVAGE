import starsalvage.engine.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestGameEngineJ4 {

    @Test public void testGetSize() {
        assertEquals(10, new GameEngine(10).getSize());
    }
    @Test public void testGetSizeSmall() {
        assertEquals(5, new GameEngine(5, 1L).getSize());
    }
    @Test public void testPlayerStartsAtOrigin() {
        GameEngine ge = new GameEngine(10, 42L);
        assertEquals(0, ge.getPlayer().getRow());
        assertEquals(0, ge.getPlayer().getCol());
    }
    @Test public void testExitAtBottomRight() {
        GameEngine ge = new GameEngine(10, 42L);
        assertEquals(CellType.EXIT, ge.getMap()[9][9].getType());
    }
    @Test public void testMovePlayerSouth() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[1][0].setType(CellType.EMPTY);
        assertTrue(ge.movePlayer(Direction.SOUTH));
        assertEquals(1, ge.getPlayer().getRow());
    }
    @Test public void testMovePlayerEast() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.EMPTY);
        assertTrue(ge.movePlayer(Direction.EAST));
        assertEquals(1, ge.getPlayer().getCol());
    }
    @Test public void testMoveBlockedByAsteroid() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.ASTEROID);
        assertFalse(ge.movePlayer(Direction.EAST));
        assertEquals(0, ge.getPlayer().getCol());
    }
    @Test public void testMoveOutOfBoundsNorth() {
        assertFalse(new GameEngine(10, 42L).movePlayer(Direction.NORTH));
    }
    @Test public void testMoveOutOfBoundsWest() {
        assertFalse(new GameEngine(10, 42L).movePlayer(Direction.WEST));
    }
    @Test public void testCollectFuel() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1] = new Cell(CellType.DEBRIS, Item.FUEL);
        ge.movePlayer(Direction.EAST);
        assertTrue(ge.getPlayer().getFuel() > 0);
        assertEquals(CellType.EMPTY, ge.getMap()[0][1].getType());
    }
    @Test public void testCollectShield() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1] = new Cell(CellType.DEBRIS, Item.SHIELD);
        ge.movePlayer(Direction.EAST);
        assertTrue(ge.getPlayer().getShields() > 0);
    }
    @Test public void testCollectWeapon() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1] = new Cell(CellType.DEBRIS, Item.WEAPON);
        ge.movePlayer(Direction.EAST);
        assertEquals(1, ge.getPlayer().getWeapons());
    }
    @Test public void testCollectCredits() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1] = new Cell(CellType.DEBRIS, Item.CREDITS);
        ge.movePlayer(Direction.EAST);
        assertEquals(10, ge.getPlayer().getCredits());
    }
    @Test public void testTurretStartsIdle() {
        GameEngine ge = new GameEngine(10, 42L);
        for (var t : ge.getTurrets()) assertEquals(TurretState.IDLE, t.getState());
    }
    @Test public void testTurretCycleFull() {
        Turret t = new Turret(3, 3);
        t.advanceState(); assertEquals(TurretState.CHARGING, t.getState());
        t.advanceState(); assertEquals(TurretState.FIRING,   t.getState());
        t.advanceState(); assertEquals(TurretState.IDLE,     t.getState());
        t.advanceState(); assertEquals(TurretState.CHARGING, t.getState());
    }
    @Test public void testTurretDoesNotShootWhenIdle() {
        assertFalse(new Turret(0, 5).canShootAt(0, 0));
    }
    @Test public void testTurretDoesNotShootWhenCharging() {
        Turret t = new Turret(0, 5); t.advanceState();
        assertFalse(t.canShootAt(0, 0));
    }
    @Test public void testTurretShootsWhenFiring() {
        Turret t = new Turret(0, 5); t.advanceState(); t.advanceState();
        assertTrue(t.canShootAt(0, 0));
        assertTrue(t.canShootAt(3, 5));
        assertFalse(t.canShootAt(3, 3));
    }
    @Test public void testShieldsAbsorbDamage() {
        Player p = new Player(0, 0);
        p.collectItem(Item.SHIELD);
        int hp = p.getHealth();
        p.takeDamage(2);
        assertEquals(0, p.getShields());
        assertEquals(hp, p.getHealth());
    }
    @Test public void testDamagePenetratesWhenNoShields() {
        Player p = new Player(0, 0);
        p.takeDamage(3);
        assertEquals(Player.MAX_HEALTH - 3, p.getHealth());
    }
    @Test public void testPlayerDeathCondition() {
        Player p = new Player(0, 0);
        p.takeDamage(Player.MAX_HEALTH);
        assertTrue(p.isDead());
    }
    @Test public void testShootTurretAdjacent() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getTurrets().forEach(t -> { t.setAlive(false); ge.getMap()[t.getRow()][t.getCol()].setType(CellType.EMPTY); });
        ge.getMap()[0][1].setType(CellType.TURRET);
        ge.getPlayer().collectItem(Item.WEAPON);
        assertTrue(ge.shootTurret(Direction.EAST));
        assertEquals(CellType.EMPTY, ge.getMap()[0][1].getType());
    }
    @Test public void testShootTurretNoWeapon() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.TURRET);
        assertFalse(ge.shootTurret(Direction.EAST));
    }
    @Test public void testWaitAdvancesTurn() {
        GameEngine ge = new GameEngine(10, 42L);
        int b = ge.getTurn(); ge.waitTurn();
        assertEquals(b + 1, ge.getTurn());
    }
    @Test public void testUndoRestoresPosition() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.EMPTY);
        ge.movePlayer(Direction.EAST);
        assertEquals(1, ge.getPlayer().getCol());
        ge.undo();
        assertEquals(0, ge.getPlayer().getCol());
    }
    @Test public void testUndoRestoresInventory() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1] = new Cell(CellType.DEBRIS, Item.WEAPON);
        ge.movePlayer(Direction.EAST);
        assertEquals(1, ge.getPlayer().getWeapons());
        ge.undo();
        assertEquals(0, ge.getPlayer().getWeapons());
    }
    @Test public void testUndoMultipleLevels() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.EMPTY);
        ge.getMap()[0][2].setType(CellType.EMPTY);
        ge.movePlayer(Direction.EAST);
        ge.movePlayer(Direction.EAST);
        ge.undo(); ge.undo();
        assertEquals(0, ge.getPlayer().getCol());
    }
    @Test public void testUndoWhenNothingToUndo() {
        assertFalse(new GameEngine(10, 42L).undo());
    }
    @Test public void testTurnIncrementOnMove() {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.EMPTY);
        int b = ge.getTurn(); ge.movePlayer(Direction.EAST);
        assertEquals(b + 1, ge.getTurn());
    }
    @Test public void testSaveAndLoad() throws Exception {
        GameEngine ge = new GameEngine(10, 42L);
        ge.getMap()[0][1].setType(CellType.EMPTY);
        ge.movePlayer(Direction.EAST);
        String path = System.getProperty("java.io.tmpdir") + "/test_ss.sav";
        ge.saveGame(path);
        GameEngine loaded = GameEngine.loadGame(path);
        assertEquals(ge.getPlayer().getCol(), loaded.getPlayer().getCol());
        assertEquals(ge.getTurn(), loaded.getTurn());
    }
    @Test public void testUseWeaponReturnsFalseWhenEmpty() {
        assertFalse(new Player(0,0).useWeapon());
    }
    @Test public void testUseWeaponDecrementsCount() {
        Player p = new Player(0,0);
        p.collectItem(Item.WEAPON); p.collectItem(Item.WEAPON);
        assertTrue(p.useWeapon());
        assertEquals(1, p.getWeapons());
    }
    @Test public void testDeadTurretDoesNotAdvance() {
        Turret t = new Turret(3, 3); t.setAlive(false); t.advanceState();
        assertEquals(TurretState.IDLE, t.getState());
    }
    @Test public void testDeadTurretCannotShoot() {
        Turret t = new Turret(0, 5); t.setState(TurretState.FIRING); t.setAlive(false);
        assertFalse(t.canShootAt(0, 0));
    }
}
