package chip8;

public class KeyBoard {
	public static String[] board = new String[] {
		"1", "2", "3", "4",
		"q", "w", "e", "r",
		"a", "s", "d", "f",
		"z", "x", "c", "v"
	};
	
	private byte[] keys = new byte[16];
	
	public KeyBoard() {
	}
	
	public void setKey(String t) {
		if (inSet(t)) {
			keys[boardToPad(t)] = 1;
		}
	}
	public void unsetKey(String t) {
		if (inSet(t)) {
			keys[boardToPad(t)] = 0;
		}
	}
	public byte getKey(int index) {
		return keys[index];
	}
	public byte[] getKeys() {
		return keys;
	}
	public boolean inSet(String a) {
		for(int i=0; i < board.length; i++) {
			if (a.equals(board[i]))
				return true;
		}
		return false;
	}
	public int boardToPad(String a) {
		byte b;
		switch (a.charAt(0)) {
			case '1':
				b = 0x1;
				break;
			case '2':
				b = 0x2;
				break;
			case '3':
				b = 0x3;
				break;
			case '4':
				b = 0xC;
				break;
			case 'q':
				b = 0x4;
				break;
			case 'w':
				b = 0x5;
				break;
			case 'e':
				b = 0x6;
				break;
			case 'r':
				b = 0xD;
				break;
			case 'a':
				b = 0x7;
				break;
			case 's':
				b = 0x8;
				break;
			case 'd':
				b = 0x9;
				break;
			case 'f':
				b = 0xE;
				break;
			case 'z':
				b = 0xA;
				break;
			case 'x':
				b = 0x0;
				break;
			case 'c':
				b = 0xB;
				break;
			case 'v':
				b = 0xF;
				break;
			default:
				b = 0x0;
				break;
		}
		return b;
	}
}
