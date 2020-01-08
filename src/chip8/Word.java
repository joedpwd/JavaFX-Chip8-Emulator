package chip8;

public class Word {
	protected int val;
	public Word(int src) {
		setWord(src);
	}
	public void setWord(int src) {
		val = src & 0xffff;
	}
	public int getWord() {
		return val;
	}
}
