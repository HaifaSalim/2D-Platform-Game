
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

import java.io.*;

import game2D.*;

// Game demonstrates how we can override the GameCore class
// to create our own 'game'. We usually need to implement at
// least 'draw' and 'update' (not including any local event handling)
// to begin the process. You should also add code to the 'init'
// method that will initialise event handlers etc. 

// Student ID: 3141979

@SuppressWarnings("serial")

public class Game extends GameCore {

	// Useful game constants
	static int screenWidth = 512;
	static int screenHeight = 384;

	// Game constants
	// float lift = 0.005f;
	float gravity = 0.0001f;
	float fly = -0.02f;
	float moveSpeed = 0.05f;

   //layers of the background for parallax scrolling
	private Image bgImage;
	private Image bgImage2;
	private Image bgImage3;
	private Image bgImage4;
	
	// Game state flags
	boolean jump = false;
	boolean moveRight = false;
	boolean moveLeft = false;
	boolean debug = true;
	boolean attack = false;
	boolean defeated = false;
	boolean movingRight = true;

	// Game resources
	Animation landing;
	Animation moving;
	Animation attacking;
	Animation jumping;
	Animation backwards;
	Animation yeti_moving;
	Animation jumping_back;
	
	//sprites 
	Sprite player = null;
	Sprite yeti;
	Sprite yeti2;
	ArrayList<Sprite> clouds = new ArrayList<Sprite>();
	
	ArrayList<Tile> collidedTiles = new ArrayList<Tile>();
	TileMap tmap = new TileMap(); // Our tile map, note that we load it in init()
	
	private int level = 1;
	int coins = 0;
	long total; // The score will be the total time elapsed since a crash

	/**
	 * The obligatory main method that creates an instance of our class and starts
	 * it running
	 * 
	 * @param args The list of parameters this program might use (ignored)
	 */
	public static void main(String[] args) {

		Game gct = new Game();
		gct.init();
		// Start in windowed mode with the given screen height and width
		gct.run(false, screenWidth, screenHeight);
	}

	/**
	 * Initialise the class, e.g. set up variables, load images, create animations,
	 * register event handlers.
	 * 
	 * This shows you the general principles but you should create specific methods
	 * for setting up your game that can be called again when you wish to restart
	 * the game (for example you may only want to load animations once but you could
	 * reset the positions of sprites each time you restart the game).
	 */
	public void init() {
		backgroundMusic("sounds/Theme.mid");
		Sprite s; // Temporary reference to a sprite
		// Load the tile map and print it out so we can check it is valid
		tmap.loadMap("maps", "map.txt");
		tmap.loadMap("maps", "map2.txt");
		setSize(tmap.getPixelWidth() / 4, tmap.getPixelHeight());
		setVisible(true);

		bgImage = loadImage("images/Sky.png");
		bgImage2 = loadImage("images/BG.png");
		bgImage3 = loadImage("images/Middle.png");
		bgImage4 = loadImage("images/Foreground.png");

		// Create a set of background sprites that we can
		// rearrange to give the illusion of motion

		landing = new Animation();
		landing.loadAnimationFromSheet("images/idle.png", 5, 1, 82);

		// Initialise the player with an animation
		player = new Sprite(landing);
        //Different player animations
		moving = new Animation();
		moving.loadAnimationFromSheet("images/moving.png", 8, 1, 60);

		backwards = new Animation();
		backwards.loadAnimationFromSheet("images/movingback.png", 8, 1, 60);

		jumping = new Animation();
		jumping.loadAnimationFromSheet("images/jump.png", 1, 1, 50);
		jumping_back = new Animation();
		jumping_back.loadAnimationFromSheet("images/jump2.png", 1, 1, 50);

		attacking = new Animation();
		attacking.loadAnimationFromSheet("images/kick.png", 1, 1, 60);

		yeti_moving = new Animation();
		yeti_moving.loadAnimationFromSheet("images/yetiwalking.png", 8, 1, 60);
		yeti_moving.setAnimationSpeed(0.50f);

		yeti = new Sprite(yeti_moving);
		yeti.setVelocityX(0.025f);

		yeti2 = new Sprite(yeti_moving);
		yeti.setVelocityX(0.025f);

		// Load a single cloud animation
		Animation ca = new Animation();
		ca.addFrame(loadImage("images/cloud.png"), 1000);

		// Create 3 clouds at random positions off the screen
		// to the right
		for (int c = 0; c < 3; c++) {
			s = new Sprite(ca);
			s.setX(screenWidth + (int) (Math.random() * 200.0f));
			s.setY(30 + (int) (Math.random() * 150.0f));
			s.setVelocityX(-0.02f);
			s.show();
			clouds.add(s);
		}

		initialiseGame();

		System.out.println(tmap);
	}

	/**
	 * You will probably want to put code to restart a game in a separate method so
	 * that you can call it when restarting the game when the player loses.
	 */
	public void initialiseGame() {

		// Loads the map based on the current level 
		switch (level) {
		case 1:
			tmap.loadMap("maps", "map.txt"); //setting the yetis position and velocity
			yeti.setPosition(650, 200); 
			yeti.setVelocityX(0.015f);
			yeti.show();
			yeti2.setPosition(700, 200);
			yeti2.setVelocityX(0.015f);
			yeti2.show();
			total = 0;
			coins = 0;
			break;
		case 2:
			tmap.loadMap("maps", "map2.txt");
			total = 0; //resetting the total and the coin count
			coins = 0;
			break;
		default:
			System.out.println("Congrats! You have completed all the levels.");
			break;
		}
		player.setPosition(200, 200); //setting player position and velocity same for all levels
		player.setVelocity(0, 0);
		player.show();
	}

	/**
	 * Draw the current state of the game. Note the sample use of debugging output
	 * that is drawn directly to the game screen.
	 */
	public void draw(Graphics2D g) {
		// Be careful about the order in which you draw objects - you
		// should draw the background first, then work your way 'forward'

		// First work out how much we need to shift the view in order to
		// see where the player is. To do this, we adjust the offset so that
		// it is relative to the player's position along with a shift
		int xo = -(int) player.getX() + 200;
		int yo = 0;
		
		if (player.getX() >= 1824) { // adjusting the offsets 
			xo = -(int) 1640;
		} else if (xo > 0) {
			xo = 0;
		}

		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());

		// Drawing the backgrounds with parallax scrolling
		g.drawImage(bgImage, (int) (xo * 0.2f), (int) (yo * 0.3f), 1500, 600, null); 
		g.drawImage(bgImage2, (int) (xo * 0.3f), (int) (yo * 0.5f), 1500, 700, null); 
		g.drawImage(bgImage3, (int) (xo * 0.5f), (int) (yo * 0.7f), 1500, 800, null); 
		g.drawImage(bgImage4, (int) (xo * 0.7f), (int) (yo * 0.9f), 1500, 800, null); 
		

		// Applying offsets to sprites then drawing them
		for (Sprite s : clouds) {
			s.setOffsets(xo, yo);
			s.draw(g);
		}
		if (level == 1) {
			yeti.setOffsets(xo, yo);
			yeti.draw(g);
			yeti2.setOffsets(xo, yo);
			yeti2.draw(g);
		}

		// Apply offsets to tile map and draw it
		tmap.draw(g, xo, yo);

		// Apply offsets to player and draw
		player.setOffsets(xo, yo);
		player.draw(g);

		// Show score and coins collected
		String msg = String.format("Score: %d", total / 100);
		g.setColor(Color.blue);
		g.drawString(msg, getWidth() - 100, 70);

		String coin = String.format("Coins: %d", coins);
		g.setColor(Color.blue);
		g.drawString(coin, getWidth() - 100, 90);
		if (debug) {

			// When in debug mode, you could draw borders around objects
			// and write messages to the screen with useful information.
			// Try to avoid printing to the console since it will produce
			// a lot of output and slow down your game.
			tmap.drawBorder(g, xo, yo, Color.black);

			g.setColor(Color.red);
			// player.drawBoundingBox(g);

			//g.drawString(String.format("Player: %.0f,%.0f", player.getX(), player.getY()), getWidth() - 100, 70);

			// drawCollidedTiles(g, tmap, xo, yo);
			// if (player.getX() >= tmap.getPixelWidth() - player.getWidth()) {
			// stop(); // Stop the game loop
		}
	}

	/**
	 * This method plays background music from a midi file given
	 * 
	 * @param filename: path of the midi file
	 */
	public static void backgroundMusic(String filename) {
		try {
			File file = new File(filename);
			Sequence sequence = MidiSystem.getSequence(file); //reading the midi file
			Sequencer sequencer = MidiSystem.getSequencer();

			sequencer.open(); //opening the sequencer
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY); //looping
			sequencer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method plays an attack sound effect and applies a fade filter to it
	 * 
	 * @param filename: path of the audio file
	 */
	private void AttackSound(String filename) {
	    try {
	        
	        File file = new File(filename);
	        AudioInputStream stream = AudioSystem.getAudioInputStream(file); //getting the inputstream from the file
	        AudioFormat format = stream.getFormat();

	        FadeFilterStream filtered = new FadeFilterStream(stream); // Applying fade filter to the audio
	        AudioInputStream f = new AudioInputStream(filtered, format, stream.getFrameLength());
	        DataLine.Info info = new DataLine.Info(Clip.class, format);

	        Clip clip = (Clip) AudioSystem.getLine(info);
	        clip.open(f);
	        clip.start();

	    } catch (Exception e) {
	      
	        e.printStackTrace();
	    }
	}

	public void drawCollidedTiles(Graphics2D g, TileMap map, int xOffset, int yOffset) {
		if (collidedTiles.size() > 0) {
			int tileWidth = map.getTileWidth();
			int tileHeight = map.getTileHeight();

			g.setColor(Color.blue);
			for (Tile t : collidedTiles) {
				g.drawRect(t.getXC() + xOffset, t.getYC() + yOffset, tileWidth, tileHeight);
			}
		}
	}
	

	/**
	 * Update any sprites and check for collisions
	 * 
	 * @param elapsed The elapsed time between this call and the previous call of
	 *                elapsed
	 */
	public void update(long elapsed) {
		  // Make adjustments to the speed of the sprite due to gravity
		player.setVelocityY(player.getVelocityY() + (gravity * elapsed));
		yeti.setVelocityY(yeti.getVelocityY() + (gravity * elapsed));
		yeti2.setVelocityY(yeti2.getVelocityY() + (gravity * elapsed));
		
		total += elapsed; //total elapsed time

		player.setAnimationSpeed(1.30f);

		if (jump) {
			player.setAnimationSpeed(1.8f);
			player.setVelocityY(fly);
		}
		
		if (player.getY() > tmap.getPixelHeight()) { //checks if the player falls 
			playerDeath(); // Reset the game if the player has fallen off
		}
		
		if (moveRight) {
			player.setVelocityX(moveSpeed);
		} else if (moveLeft) {
			player.setVelocityX(-moveSpeed);
		} else {
			player.setVelocityX(0);
		}
		
		if (attack) {
			if (attackingCollision(player, yeti, 20)) { //checking for collision with yeti
				yeti.hide(); //the sprite is hidden
				defeated = true;
			}
			if (attackingCollision(player, yeti2, 20)) {
				yeti2.hide();//the sprite is hidden
				defeated = true;
			}

		}

		if (yeti.getX() > tmap.getPixelWidth() && yeti2.getX() > tmap.getPixelWidth()) { //handle yeti if they go off the screen
			yeti.setX(-yeti.getWidth());
			yeti2.setX(-yeti2.getWidth());

		}
		if (!defeated) {
			// Checking for collisions with the yetis
			if (attackingCollision(player, yeti, 10)) {
				playerDeath();//the player dies and game restarts
			}
			if (attackingCollision(player, yeti2, 10)) {
				playerDeath();//the player dies and game restarts
			}
		}

		for (Sprite s : clouds)
			s.update(elapsed);

		// Updating player and yeti sprites
		player.update(elapsed);
		yeti.update(elapsed);
		yeti2.update(elapsed);

		// Check for collisions with the tile map
		checkTileCollision(player, tmap);
		checkTileCollision(yeti, tmap);
		checkTileCollision(yeti2, tmap);

		if (level > 2) {
		    try {
		        // Sleep for 2 seconds before stopping the game
		        Thread.sleep(2000);
		    } catch (InterruptedException e) {
		        e.printStackTrace();
		    }
		    stop(); // Stop the game after the delay
		}

	}
	
	/** Method to handle players death and restarting the game when the player dies
	 * 
	 */
	public void playerDeath() {
		if (level == 1) {
			initialiseGame();
			
		} else if (level == 2) {
			initialiseGame();
		}
	}
	
	/** Modifying the sample code of bounding box collision in the lecture notes to properly detect
	 * a collision between sprites s1 and s2, with a distance specified between them.
	 * 
	 * @return	true if a collision may have occurred, false if it has not.
	 */
	public boolean attackingCollision(Sprite s1, Sprite s2, int dist) {
		return ((s1.getX() + s1.getImage().getWidth(null) + dist > s2.getX())
				&& (s1.getX() - dist < (s2.getX() + s2.getImage().getWidth(null)))
				&& ((s1.getY() + s1.getImage().getHeight(null) + dist > s2.getY())
						&& (s1.getY() - dist < s2.getY() + s2.getImage().getHeight(null))));
	}

	public void checkTileCollision(Sprite s, TileMap tmap) {
		// Clear any previously detected collision tiles
		collidedTiles.clear();

		float sx = s.getX() + s.getVelocityX();
		float sy = s.getY() + s.getVelocityY();
		
    	// Find out how wide and how tall a tile is
    	float tileWidth = tmap.getTileWidth();
    	float tileHeight = tmap.getTileHeight();

		int xtile = (int) (sx / tileWidth );
		int ytile = (int) (sy /tileHeight );
		int rightTile = (int) ((sx + s.getWidth()) /tileWidth );
		int bottomTile = (int) ((sy + s.getHeight()) / tileHeight );
	
		for (int x = xtile; x <= rightTile; x++) {  // Iterating over the tiles that could collide with the sprite
			for (int y = ytile; y <= bottomTile; y++) {
				Tile t1 = tmap.getTile(x, y);

				if (t1 != null && (t1.getCharacter() != '.' && t1.getCharacter() != 's')) {
					// creating rectangles for the tiles collision box and the sprites position
					Rectangle tbounds = new Rectangle (x * tmap.getTileWidth(), y * tmap.getTileHeight(), 
							tmap.getTileWidth(), tmap.getTileHeight());

					Rectangle fbounds = new Rectangle((int) sx, (int) sy, s.getWidth(), s.getHeight());

					if (fbounds.intersects(tbounds)) {   // Check for intersection between sprite and tile 
						if (t1.getCharacter() == 'c') {  // Handling the collision with coin
							float coinsx = tbounds.x + tbounds.width / 2; // Center coordinates of the coin
							float coinsy = tbounds.y + tbounds.height / 2;
							
							float dist = (float) Math.sqrt(Math.pow(sx + s.getWidth() / 2 - coinsx, 2)
								+ Math.pow(sy + s.getHeight() / 2 - coinsy, 2)); // Calculating the distance between sprite and center of the coin
							
							if (dist <= 20f) {
								Sound s1 = new Sound("sounds/coin2.wav");  // Playing the coin sound
								s1.start();
								tmap.setTileChar('.', x, y); // removing the coins and incrementing the count after
								coins++;
							}
						} else if (t1.getCharacter() == 'x') {
							Sound s1 = new Sound("sounds/win.wav");  // Playing sound for finishing a level
							s1.start();
							level++; //the next level starts after this tile is reached
							initialiseGame();
						} else {
							// for handling other collisions
							handlingTileCollision(s, t1, tbounds);
							collidedTiles.add(t1);
						}
					}
				}
			}
		}
	}
	   

	/**
	 * Checks and handles collisions with the edge of the screen. You should
	 * generally use tile map collisions to prevent the player leaving the game
	 * area. This method is only included as a temporary measure until you have
	 * properly developed your tile maps.
	 * 
	 * @param s       The Sprite to check collisions for
	 * @param tmap    The tile map to check
	 * @param elapsed How much time has gone by since the last call
	 */
	public void handleScreenEdge(Sprite s, TileMap tmap, long elapsed) {
		// This method just checks if the sprite has gone off the bottom screen.
		// Ideally you should use tile collision instead of this approach

		float difference = s.getY() + s.getHeight() - tmap.getPixelHeight();
		if (difference > 0) {
			// Put the player back on the map according to how far over they were
			s.setY(tmap.getPixelHeight() - s.getHeight() - (int) (difference));

			// and make them bounce
			s.setVelocityY(-s.getVelocityY() * 0.75f);
		}
	}


	/**
	 * Override of the keyPressed event defined in GameCore to catch our own events
	 * also sets the animations for the players according to the key pressed
	 * @param e The event that has been generated
	 */
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		switch (key) {
		case KeyEvent.VK_UP:
			jump = true;
			if (movingRight) { //if the player is facing right
				player.setAnimation(jumping);
			} else {
				player.setAnimation(jumping_back); //if the player is facing left
			}
			break;
		case KeyEvent.VK_RIGHT:
			moveRight = true;
			player.setAnimation(moving);
			movingRight = true;
			break;
		case KeyEvent.VK_LEFT:
			moveLeft = true;
			player.setAnimation(backwards);
			movingRight = false; 
			break;
		case KeyEvent.VK_S:
			Sound s = new Sound("sounds/caw.wav");
			s.start();
			break;
		case KeyEvent.VK_ESCAPE:
			stop();
			break;
		case KeyEvent.VK_SPACE:
			attack = true;
			AttackSound("sounds/kick2.wav"); //playing the filtered attacksound
			player.setAnimation(attacking);
			break;
		case KeyEvent.VK_B:
			debug = !debug;
			break; 
		default:
			break;
		}
	}

	/**
	 * Check and handles collisions with a tile map for the given sprite 's'.
	 * @param s    The Sprite to check collisions for
	 * @param tmap The tile map to check
	 */

	public void handlingTileCollision(Sprite s, Tile tile, Rectangle bounds) {
		 // finding the horizontal and vertical overlap of the sprite and the tile bounds
		float x = Math.min(s.getX() + s.getWidth() - bounds.x, bounds.x + bounds.width - s.getX());
		float y = Math.min(s.getY() + s.getHeight() - bounds.y, bounds.y + bounds.height - s.getY());

		if (x < y) {
			if (s.getX() < bounds.x) {  // handling the horizontal collision
				s.setX(bounds.x - s.getWidth()); // Moves sprite to the left of the tile
				s.setVelocityX(-0.1f); 
			} else {
				s.setX(bounds.x + bounds.width);// Moves sprite to the right of the tile
				s.setVelocityX(0.1f); 
			}
		} else {
			if (s.getY() < bounds.y) { // handling the vertical collisions
				s.setY(bounds.y - s.getHeight()); // Moves sprite to the top of the tile
			} else {
				s.setY(bounds.y + bounds.height); // Moves sprite to the bottom of the tile
			}
			s.setVelocityY(0);
		}

	}

	public void keyReleased(KeyEvent e) {

		int key = e.getKeyCode();

		switch (key) { 
		case KeyEvent.VK_ESCAPE: stop();break;
		case KeyEvent.VK_UP    :jump = false;player.setAnimation(landing);break; //setting animation of player to landing (idle) after the release of the keys
		case KeyEvent.VK_RIGHT :moveRight = false;player.setAnimation(landing);break;
		case KeyEvent.VK_LEFT  :moveLeft = false;player.setAnimation(landing);break;
		case KeyEvent.VK_SPACE :attack = false;player.setAnimation(landing);break;
		default : break;
		}
	}
}