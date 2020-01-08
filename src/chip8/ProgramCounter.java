package chip8;

public class ProgramCounter extends Word {

	public ProgramCounter(int src) {
		super(src);
	}
	public void nextInst() {
		super.setWord(super.getWord()+2);
	}

}
