package chip8;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Chip8 extends Application{
	/*
	 * 0x000-0x1FF - Chip 8 interpreter (contains font set in emu)
	 * 0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
	 * 0x200-0xFFF - Program ROM and work RAM
	 */
	
	final static boolean DEBUG = false;
	protected Chip8 chip;
	
	final static int SCALE=10;
	protected Word opCode;
	
	protected byte[] memory = new byte[4096];
	protected byte[] V = new byte[16];
	protected byte[] romData;
	protected boolean drawFlag;
	
	Word I; 		//Index Register
	ProgramCounter pc;		//Program Counter
	
	protected byte[] gfx = new byte[64*32];
	
	byte delay_timer;
	byte sound_timer;
	
	protected Word[] stack = new Word[16];
	Word sp;
	
	KeyBoard board = new KeyBoard();
	
	byte[] chip8_fontset = new byte[]
		{ 
		  (byte)0xF0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xF0, // 0
		  (byte)0x20, (byte)0x60, (byte)0x20, (byte)0x20, (byte)0x70, // 1
		  (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x80, (byte)0xF0, // 2
		  (byte)0xF0, (byte)0x10, (byte)0xF0, (byte)0x10, (byte)0xF0, // 3
		  (byte)0x90, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0x10, // 4
		  (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x10, (byte)0xF0, // 5
		  (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x90, (byte)0xF0, // 6
		  (byte)0xF0, (byte)0x10, (byte)0x20, (byte)0x40, (byte)0x40, // 7
		  (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0xF0, // 8
		  (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x10, (byte)0xF0, // 9
		  (byte)0xF0, (byte)0x90, (byte)0xF0, (byte)0x90, (byte)0x90, // A
		  (byte)0xE0, (byte)0x90, (byte)0xE0, (byte)0x90, (byte)0xE0, // B
		  (byte)0xF0, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xF0, // C
		  (byte)0xE0, (byte)0x90, (byte)0x90, (byte)0x90, (byte)0xE0, // D
		  (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0xF0, // E
		  (byte)0xF0, (byte)0x80, (byte)0xF0, (byte)0x80, (byte)0x80  // F
		};
	
	public static void main(String[] args) {
		launch(args);
	}
	@Override
	public void start(Stage primaryStage) throws Exception {
		//setUpGraphics();
		Group root = new Group();
		Canvas canvas = new Canvas(SCALE*64, SCALE*32);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, SCALE*64, SCALE*32);
		/*WritableImage image = new WritableImage(SCALE*64, SCALE*32);
		PixelWriter pixel = image.getPixelWriter();
		ImageView gameNode = new ImageView(image);*/
		root.getChildren().add(canvas);
		Scene scene = new Scene(root);
		primaryStage.setTitle("Chip8");
		primaryStage.setScene(scene);
		primaryStage.show();
		//setUpInput();
		
		chip = new Chip8();
		
		try {
			//System.out.println("EXECUTING");
			chip.load("C:\\Users\\jxd45\\eclipse-workspace\\chip8\\rom\\PONG");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Timeline timeline = new Timeline();
		timeline.setCycleCount(Timeline.INDEFINITE);
		KeyFrame frame = new KeyFrame(Duration.seconds(0.003),
				e -> {
					//printKeys();
					if(DEBUG){
						System.out.println("Executing "+String.format("0x%04X", byteToInt(chip.memory[chip.pc.getWord()]) << 8 | byteToInt(chip.memory[chip.pc.getWord()+1]))
						+ " PC: " + chip.pc.getWord());
						//printKeys();
						timeline.stop();
					}
					chip.emulateCycle();
					if(chip.drawFlag == true)
						chip.drawGraphics(gc);
					
					//chip.setKeys();
				});
		timeline.getKeyFrames().add(frame);
		timeline.play();
		
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				/*if (timeline.getStatus() == Status.RUNNING)
					timeline.stop();
				else
					timeline.play();
				System.out.println(event.getText());*/
				chip.board.setKey(event.getText());
				timeline.play();
			}
			
		});
		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				chip.board.unsetKey(event.getText());
			}
			
		});
		/*for(int i=0; i < chip.memory.length; i++)
			System.out.println("Value at " + i + " is " + chip.memory[i]);*/
		/*while(true)*/ 
		
		/*for(int i=0; i<100; i++){
			System.out.println("Next Memory values "+(byteFormat(chip.memory[chip.pc.getWord()]) << 8) + " " + byteFormat(chip.memory[chip.pc.getWord() + 1]));
			System.out.println("Next Instruction "+String.format("0x%08X", byteFormat(chip.memory[chip.pc.getWord()]) << 8 | byteFormat(chip.memory[chip.pc.getWord()+1])));
			System.out.println("pc " + chip.pc.getWord() + " turn " + i);
			System.out.println("sp " + chip.sp.getWord() + " stack ");
			chip.emulateCycle();
			//System.out.format("pc %d\n", chip.pc.getWord());
			//System.out.format("pc %d\n", memory[pc.getWord()]);
			//System.out.println(String.format("0x%04X", (chip.memory[pc.getWord()] << 8 | chip.memory[pc.getWord() + 1])));
			System.out.println(chip.drawFlag);
			if(chip.drawFlag == true)
				//chip.drawGraphics(gc);
			Thread.sleep(100/6);
		
			//chip.setKeys();
		}*/
	}
	public Chip8() {
		pc = new ProgramCounter(0x200);
		opCode = new Word(0);
		I = new Word(0);
		sp = new Word(0);
		
		/*Everything should be initialised to 0 values
		 * at this point
		 */
		
		//load fontset
		for(int i=0; i < 80; ++i)
			memory[i+80] = chip8_fontset[i];
		
		//Reset Timers
	}
	public void drawGraphics(PixelWriter px) {
		Color color;
		for(int i=0; i < 32; i++) {
			for(int j=0; j < 64; j++) {
				if(gfx[(i*64) + j] == 0)
					color = Color.BLACK;
				else
					color = Color.WHITE;
				//System.out.println(gfx[(i*64) + j]);
				for(int x=0; x < SCALE; x++) {
					for (int y=0; y <SCALE; y++)
						px.setColor((SCALE*j)+x, (SCALE*i)+y, color);
				}
			}
		}
	}
	public void drawGraphics(GraphicsContext gc) {
		
		for(int i=0; i < 32; i++) {
			for(int j=0; j < 64; j++) {
				if(gfx[(i*64) + j] == 0)
					gc.setFill(Color.BLACK);
				else
					gc.setFill(Color.WHITE);
				//gc.fillRect(5, 5, 25, 25);
				//System.out.println("" + i + " " + j);
				gc.fillRect((j*SCALE), (i*SCALE), SCALE, SCALE);
			}
		}
	}
	public void load(String addr) throws IOException{
		Path fileLocation = Paths.get(addr);
		byte[] romData = Files.readAllBytes(fileLocation);
		System.arraycopy(romData, 0, memory, 512, romData.length);
	}
	public int byteToInt(byte n) {
		return n & 0x000000FF;
	}
	private void emulateCycle() {
		opCode.setWord(byteToInt(this.memory[pc.getWord()]) << 8 | byteToInt(this.memory[pc.getWord() + 1]));
		switch (opCode.getWord() & 0xF000) {
			case 0x0000:
				switch(opCode.getWord() & 0xFF00) {
					case 0x0000:
						switch(opCode.getWord() & 0x000F) {
							case 0x0000:
								op2(opCode);
								break;
							case 0x000E:
								op3(opCode);
								break;
						}
						break;
					default:
						op1(opCode);
				}
				break;
			case 0x1000:
				op4(opCode);
				break;
			case 0x2000:
				op5(opCode);
				break;
			case 0x3000:
				op6(opCode);
				break;
			case 0x4000:
				op7(opCode);
				break;
			case 0x5000:
				op8(opCode);
				break;
			case 0x6000:
				op9(opCode);
				break;
			case 0x7000:
				op10(opCode);
				break;
			case 0x8000:
				switch (opCode.getWord() & 0x000F) {
					case 0x0000:
						op11(opCode);
						break;
					case 0x0001:
						op12(opCode);
						break;
					case 0x0002:
						op13(opCode);
						break;
					case 0x0003:
						op14(opCode);
						break;
					case 0x0004:
						op15(opCode);
						break;
					case 0x0005:
						op16(opCode);
						break;
					case 0x0006:
						op17(opCode);
						break;
					case 0x0007:
						op18(opCode);
						break;
					case 0x000E:
						op19(opCode);
						break;
					default:
						System.out.println("Unknown opcode: " + opCode.getWord());
				}
				break;
			case 0x9000:
				op20(opCode);
				break;
			case 0xA000:
				op21(opCode);
				break;
			case 0xB000:
				op22(opCode);
				break;
			case 0xC000:
				op23(opCode);
				break;
			case 0xD000:
				op24(opCode);
				break;
			case 0xE000:
				switch (opCode.getWord() & 0x00FF) {
					case 0x009E:
						op25(opCode);
						break;
					case 0x00A1:
						op26(opCode);
						break;
					default:
						System.out.println("Unknown opcode: " + opCode.getWord());
				}
				break;
			case 0xF000:
				switch (opCode.getWord() & 0x00FF) {
					case 0x0007:
						op27(opCode);
						break;
					case 0x000A:
						op28(opCode);
						break;
					case 0x0015:
						op29(opCode);
						break;
					case 0x0018:
						op30(opCode);
						break;
					case 0x001E:
						op31(opCode);
						break;
					case 0x0029:
						op32(opCode);
						break;
					case 0x0033:
						op33(opCode);
						break;
					case 0x0055:
						op34(opCode);
						break;
					case 0x0065:
						op35(opCode);
						break;
					default:
						System.out.println("Unknown opcode: " + opCode.getWord());
				}
				break;
			default:
				System.out.println("Unknown opcode: " + opCode.getWord());
				
		}
		if(delay_timer > 0)
			--delay_timer;
		
		if(sound_timer > 0)
		{
			if(sound_timer == 1)
				System.out.println("Beep");
			--sound_timer;
		}
	}
	public void op1(Word op){
		pc.nextInst();
	}
	public void op2(Word op){
		for(int i=0; i < 64*32; i++) {
			gfx[i] = 0;
		}
		drawFlag = true;
		pc.nextInst();
	}
	public void op3(Word op){
		int StackPointer = sp.getWord();
		pc.setWord(stack[--StackPointer].getWord());
		sp.setWord(StackPointer);
		
		if(DEBUG) {
			System.out.println("pc -> " + pc.getWord());
			System.out.println("sp -> " + sp.getWord());
		}
	}
	public void op4(Word op){
		pc.setWord(opCode.getWord() & 0x0FFF);
	}
	public void op5(Word op){
		stack[sp.getWord()] = new Word(pc.getWord()+2);
		sp.setWord(sp.getWord()+1);
		pc.setWord(op.getWord() & 0x0FFF); 
		if(DEBUG) {
			System.out.println("pc -> " + pc.getWord());
			System.out.println("sp -> " + sp.getWord());
		}
	}
	public void op6(Word op){
		if (byteToInt(V[(op.getWord() & 0x0F00) >> 8]) == (op.getWord() & 0x00FF))
			pc.setWord(pc.getWord() + 4);
		else
			pc.nextInst();
		if(DEBUG)
			System.out.println("V" + ((op.getWord() & 0x0F00) >> 8) + " -> " + byteToInt(V[(op.getWord() & 0x0F00) >> 8]));
	}
	public void op7(Word op){
		if (byteToInt(V[(op.getWord() & 0x0F00) >> 8]) != (op.getWord() & 0x00FF))
			pc.setWord(pc.getWord() + 4);
		else
			pc.nextInst();
	}
	public void op8(Word op){
		if (V[(op.getWord() & 0x0F00) >> 8] == V[(op.getWord() & 0x00F0) >> 4])
			pc.setWord(pc.getWord() + 4);
		else
			pc.nextInst();
	}
	public void op9(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = (byte)(op.getWord() & 0x00FF);
		pc.nextInst();
		if (DEBUG)
			printReg();
	}
	public void op10(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = (byte)((byteToInt(V[(op.getWord() & 0x0F00) >> 8]) + (op.getWord() & 0x00FF)) & 0x00FF);
		pc.nextInst();
		if (DEBUG)
			printReg();
	}
	public void op11(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = V[(op.getWord() & 0x00F0) >> 4];
		pc.nextInst();
	}
	public void op12(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = (byte)((V[(op.getWord() & 0x00F0) >> 4]) | (V[(op.getWord() & 0x0F00) >> 8]));
		pc.nextInst();
	}
	public void op13(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = (byte)((V[(op.getWord() & 0x00F0) >> 4]) & (V[(op.getWord() & 0x0F00) >> 8]));
		pc.nextInst();
	}
	public void op14(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = (byte)((V[(op.getWord() & 0x00F0) >> 4]) ^ (V[(op.getWord() & 0x0F00) >> 8]));
		pc.nextInst();
	}
	public void op15(Word op){
		if(DEBUG) {
			System.out.println("V" + ((op.getWord() & 0x0F00) >> 8) + " -> " + byteToInt(V[(op.getWord() & 0x0F00) >> 8])
					+ " V" + ((op.getWord() & 0x00F0) >> 4) + " -> " +byteToInt(V[(op.getWord() & 0x00F0) >> 4]));
		}
		if ((byteToInt(V[(op.getWord() & 0x0F00) >> 8]) + byteToInt(V[(op.getWord() & 0x00F0) >> 4])) > 0xFF) {
			V[(op.getWord() & 0x0F00) >> 8] = (byte)((byteToInt(V[(op.getWord() & 0x0F00) >> 8]) + byteToInt(V[(op.getWord() & 0x00F0) >> 4])) % 0xFF);
			V[0xF] = 1;
		} else {
			V[(op.getWord() & 0x0F00) >> 8] = (byte)(byteToInt(V[(op.getWord() & 0x0F00) >> 8]) + byteToInt(V[(op.getWord() & 0x00F0) >> 4]));
			V[0xF] = 0;
		}
		pc.nextInst();
		if(DEBUG) {
			System.out.println("AFTER V" + ((op.getWord() & 0x0F00) >> 8) + " -> " + byteToInt(V[(op.getWord() & 0x0F00) >> 8]));
		}
	}
	public void op16(Word op){
		if ((byteToInt(V[(op.getWord() & 0x0F00) >> 8]) - byteToInt(V[(op.getWord() & 0x00F0) >> 4])) < 0) {
			V[(op.getWord() & 0x0F00) >> 8] = 0;
			V[0xF] = 0;
		} else {
			V[(op.getWord() & 0x0F00) >> 8] = (byte)(byteToInt(V[(op.getWord() & 0x0F00) >> 8]) - byteToInt(V[(op.getWord() & 0x00F0) >> 4]));
			V[0xF] = 1;
		}
		pc.nextInst();
	}
	public void op17(Word op){
		V[0xF] = (byte)(byteToInt(V[(op.getWord() & 0x0F00) >> 8]) & 0x1);
		V[(op.getWord() & 0x0F00) >> 8] = (byte)(byteToInt(V[(op.getWord() & 0x0F00) >> 8]) >>> 1);
		pc.nextInst();
	}
	public void op18(Word op){
		if (byteToInt(V[(op.getWord() & 0x00F0) >> 4]) - byteToInt((V[(op.getWord() & 0x0F00) >> 8])) < 0) {
			V[(op.getWord() & 0x0F00) >> 8] = 0;
			V[0xF] = 0;
		} else {
			V[(op.getWord() & 0x0F00) >> 8] = (byte)(byteToInt(V[(op.getWord() & 0x00F0) >> 4]) - byteToInt((V[(op.getWord() & 0x0F00) >> 8])));
			V[0xF] = 1;
		}
		pc.nextInst();
	}
	public void op19(Word op){
		V[0xF] = (byte)(byteToInt(V[(op.getWord() & 0x0F00) >> 8]) & 0x8);
		V[(op.getWord() & 0x0F00) >> 8] = (byte)(byteToInt(V[(op.getWord() & 0x0F00) >> 8]) << 1);
		pc.nextInst();
	}
	public void op20(Word op){
		if (V[(op.getWord() & 0x0F00) >> 8] != V[(op.getWord() & 0x00F0) >> 4])
			pc.setWord(pc.getWord() + 4);
		else
			pc.nextInst();
	}
	public void op21(Word op){
		I.setWord(opCode.getWord() & 0x0FFF);
		pc.nextInst();
		if(DEBUG)
			printI();
	}
	public void op22(Word op){
		pc.setWord(V[0x0] + (pc.getWord() & 0x0FFF));
	}
	public void op23(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = (byte) ((new Random().nextInt(256)) & (op.getWord() & 0x00FF));
		pc.nextInst();
		if (DEBUG)
			System.out.println("V" + ((op.getWord() & 0x0F00) >> 8) + " -> " + byteToInt(V[(op.getWord() & 0x0F00) >> 8]));
	}
	public void op24(Word op){
		byte x = V[(op.getWord() & 0x0F00) >> 8];
		byte y = V[(op.getWord() & 0x00F0) >> 4];
		int height = (op.getWord() & 0x000F);
		byte pixel;
		//System.out.println("x "+ byteToInt(x) + " y " + byteToInt(y) + " height " + height);
		
		V[0xF] = 0;
		for (int yline=0; yline < height; yline++) {
			pixel = memory[I.getWord() + yline];
			for (int xline=0; xline < 8; xline++) {
				if((byteToInt(pixel) & (0x80 >> xline)) != 0) {
					//System.out.println("x "+ byteFormat(x) + " y " + byteFormat(y) + " height " + height);
					//System.out.println(byteFormat(x) + xline + ((byteFormat(y) + yline) * 64));
					try {
						if (gfx[x + xline + ((y + yline) * 64)] == 1)
							V[0xF] = 1;
						gfx[x + xline + ((y + yline) * 64)] ^= 1;
					} catch(ArrayIndexOutOfBoundsException e) {
						
					}
				}
			}
		}
		
		drawFlag = true;
		pc.nextInst();
	}
	public void op25(Word op){
		if(board.getKey(byteToInt(V[(op.getWord() & 0x0F00) >> 8]))==1)
			pc.setWord(pc.getWord()+4);
		else
			pc.nextInst();
		if(DEBUG)
			printKeys();
	}
	public void op26(Word op){
		if(board.getKey(byteToInt(V[(op.getWord() & 0x0F00) >> 8]))==0) {
			pc.setWord(pc.getWord()+4);
			//System.out.println(board.getKey("LOOK" + byteToInt(V[(op.getWord() & 0x0F00)]) >> 8));
		}else
			pc.nextInst();
		if(DEBUG)
			printKeys();
	}
	public void op27(Word op){
		V[(op.getWord() & 0x0F00) >> 8] = delay_timer;
		pc.nextInst();
		
		if(DEBUG)
			System.out.println("V " + ((op.getWord() & 0x0F00) >> 8) + " -> " + byteToInt(V[(op.getWord() & 0x0F00) >> 8]));
	}
	public void op28(Word op){
		for (int i=0; i<board.getKeys().length; i++) {
			if(board.getKey(i)==1) {
				V[(op.getWord() & 0x0F00) >> 8] = (byte) i;
				pc.nextInst();
				return;
			}
		}
		return;
	}
	public void op29(Word op){
		delay_timer = V[(op.getWord() & 0x0F00) >> 8];
		pc.nextInst();
		if(DEBUG)
			System.out.println(byteToInt(delay_timer));
	}
	public void op30(Word op){
		sound_timer = V[(op.getWord() & 0x0F00) >> 8];
		pc.nextInst();
	}
	public void op31(Word op){
		I.setWord(I.getWord()+byteToInt(V[(op.getWord() & 0x0F00) >> 8]));
		pc.nextInst();
		if (DEBUG){
			System.out.println("I -> " + I.getWord());
		}
	}
	public void op32(Word op){
		I.setWord(80 + (byteToInt(V[(op.getWord() & 0x0F00) >> 8])*5));
		pc.nextInst();
		if(DEBUG)
			printI();
	}
	public void op33(Word op){
		int dec = byteToInt((V[(op.getWord() & 0x0F00) >> 8]));
		
		if(DEBUG)
			System.out.println(dec);
		
		memory[I.getWord()] = (byte)(dec / 100);
		dec %= 100;
		
		memory[I.getWord() + 1] = (byte)(dec/10);
		dec %= 10;
		
		memory[I.getWord() + 2] = (byte)(dec);
		
		if(DEBUG) {
			for(int i=0; i < 3; i++)
				System.out.println("I + " + i + " -> " + byteToInt(memory[I.getWord()+i]));
		}
			
		pc.nextInst();
	}
	public void op34(Word op){
		int max = (op.getWord() & 0x0F00) >> 8;
		for(int i=0; i < max+1; i++) {
			memory[I.getWord()+i] = V[i];
		}
		pc.nextInst();
		if (DEBUG) {
			for(int i=0; i < max+1; i++) {
				System.out.println("I" + i + " " + byteToInt(memory[I.getWord()+i]));
			}
		}
	}
	public void op35(Word op){
		int max =(op.getWord() & 0x0F00) >> 8;
		for(int i=0; i < max+1; i++) {
			V[i] = memory[I.getWord()+i];
		}
		pc.nextInst();
		
		if(DEBUG) {
			System.out.println("I -> I + " + max);
			for(int i=0; i < max; i++)
				System.out.println("I + " + i + " -> " + byteToInt(memory[I.getWord()+i]));
			printReg();
		}
	}
	public void printReg() {
		for (int i=0 ; i < V.length; i++)
			System.out.println("V" + i + " -> " + byteToInt(V[i]));
	}
	public void printKeys() {
		for (int i=0 ; i < board.getKeys().length; i++)
			System.out.println("Key " + i + " -> " + board.getKeys()[i]);
	}
	public void printI() {
		System.out.println("I -> " + I.getWord());
	}
}
