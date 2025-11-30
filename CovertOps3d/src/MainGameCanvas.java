import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Stack;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class MainGameCanvas extends GameCanvas implements Runnable {
   public boolean isGameRunning = false;
   public boolean isGamePaused = false;
   public boolean isGameInitialized = true;
   public boolean areResourcesLoaded = false;
   public static CovertOps3D mainMidlet = null;
   private static final String[] levelFileNames = new String[]{"01a", "01b", "02a", "02b", "04", "05", "06a", "06b", "06c", "07a", "07b", "08a", "08b"};
   public static int currentLevelId = 0;
   public static int previousLevelId = -1;
   public static int keyMappingOffset;
   public static AudioManager audioManager;
   private String[] SETTINGS_MENU_ITEMS;
   private String[] chapterMenuItems;
   private static final String[] mainMenuItems = new String[]{"new game", "settings", "help", "about", "quit"};
   private static final String[] pauseMenuItems = new String[]{"resume", "new game", "settings", "help", "about", "quit"};
   private static final String[] difficultyMenuItems = new String[]{"difficulty", "", "easy", "normal", "hard", "back"};
   private static final String[] CHAPTER_MENU_DATA = new String[]{"chapter", "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "back"};
   private static final String[] CONFIRMATION_MENU_ITEMS = new String[]{"are you sure?", "", "no"};
   private static final String[] HELP_MENU_ITEMS = new String[]{"Controls:", "", "2/up - walk forward", "8/down - step backwards", "4/left - turn left", "6/right - turn right", "7 - strafe left", "9 - strafe right", "5/action - fire", "1 - open door/move lift", "3 - select weapon", "0 - toggle map"};
   private static final String[] ABOUT_MENU_TEXT = new String[]{"Covert Ops 3D", "", "Developed by:", "Micazook Mobile Ltd.", "", "Executive producers:", "Marcin Kochanowski", "Wojciech Charysz", "Michael Fotoohi", "", "Senior developer:", "Tomasz Mroczek", "", "Level design:", "Kamil Bachminski", "", "Texture artists:", "Kamil Bachminski", "Patryk Piescinski", "", "Character design:", "Lukasz 'Slizgi' Sliwinski", "Kamil Bachminski", "", "Music:", "Slawomir Opalinski", "", "Sound effects:", "Kamil Bachminski", "", "", "Publisher:", "Micazook Ltd.", "", "www.micazook.com", "", "for support email", "support@micazook.com", "", "(c) 2006 Micazook Ltd.", "Trademarks belong to", "their respective owners.", "", "All rights reserved!"};
   private static final String[][] storyText = new String[][]{{"RMy name is captain Thomas Reed. My mission, Covert Operations in service of the US army. I began my career in Spain and since then I have participated in many secret missions against the enemy. As an Allied secret agent my job is to infiltrate and sabotage behind the enemy lines. This mission is a typical one, dangerous with my name written on it!! Our spy planes have revealed photographs of what seems to be of an immense constructions project taking place around the Weissberg Mountain in the German Alps. It seems that the Germans are digging a network of reinforcements and underground bunkers on a previously unparalleled scale. My mission is to hide aboard a transportation supply train until it gets to Weissberg and to meet with our undercover agent on location there. With her help, I need to get the documents that reveals the purpose of this enterprise which incidentally the HQ nicknamed Fort Weissberg. Not sure how yet but I need to devise a way to sabotage the railway system and other important installations on site. I need some luck today and lots of it!!"}, {"ACaptain Reed I presume?", "RWho are you!?", "AAnna Sierck, MI5. I was told to meet you here in Weissberg.", "RSo why now, why in the train? If the Germans find us here, my mission is over!", "AThere has been a change of plans. Don't worry. We are safe, at least until the train stops.", "RWhat's happening, has something gone wrong?", "ANo don't worry, HQ's idea, just some last minute changes to keep the Germans guessing.", "RI knew it.", "AEver since our last failed attempt the trains are heavily searched. You'll have to leave at the last hidden station in the forest and walk to the fort by foot.", "RLast, failed attempt?? How many times have you tried so far?", "ARight now you don't need to know that. Ah and one more thing. The forts gates are not as heavily guarded as the train yard, but nevertheless you can expect a lot of resistance there. I know of a sniper rifle stored somewhere at the station. Find it and use it against the gate guards."}, {"AI see you found this rifle, good. You'll have to shoot the guards before entering Fort Weissberg.", "RAnd you?", "AI'll meet you inside. Maybe I'll be able to get a uniform for you.", "RThanks.", "AReed?", "RYes?", "AGood luck."}, {"AGlad you made it.", "RPiece of cake.", "AUnfortunately I have some bad news. There's gossip of some sort of a secret weapon undergoing tests here. I don't know if it's true, but the guard outpost's has been heavily reinforced. Uniform will not do you any good - they are checking everyone's id cards now practically on every corridor.", "RWhat do I do then?", "AYou can get deeper into the fortress through the unfinished tunnels. But you'll need explosives, as some of the passages are systematically being sealed for security reasons. I'm sure there's dynamite somewhere here. Get it and then find a wall that looks like it shouldn't be there...", "RWhat? Can't you be a little bit more precise?", "AUnfortunately our plans backfired and we couldn't get you any uniforms. Sorry bad luck old chap.", "RAll right, but this my life on the line here."}, {"AYou did it! Now all you need to do is to find the documents. I suppose they are locked somewhere in this level, perhaps you'll need to search for keys.", "RAnd what about you?", "AThere is some commotion in the base, I'll try and see what's going on. We will meet here after we're done.", "RSee you then."}, {"RI've got these papers. Can we finally blow this place up? It's giving me the creeps.", "AI'm sorry, but there's been a slight change of plans again.", "RGreat. I was longing to hear it. What's happening now?", "AHave you ever heard of Clint Miller?", "RDoctor Clint Miller? The Nobel prize winner?", "AYes That's him. A few weeks ago he disappeared from his house in Boston. He's here now, arrived today. The Germans have kidnapped him.", "RWhat? Why?", "AFrom what I know he was conducting some sort of research on the possible military uses of sound waves back in the US.", "RYou mean...", "AOuch, sonic weapons. Germans are doing similar experiments but with no success, so far at least.", "RHe must have cracked it if the German's risked kidnapping him in the states.", "AThat's what I'm afraid of. We have to get him out of here and fast.", "RWhere do they keep him.", "AOn this very level. That's where you come in. Once again you'll need to use your sniper rifle and get rid of the guards.", "RYou lead the way."}, {"RWhat now?", "AThese are the labs and prison's. Miller will be somewhere here. Be careful, I know we are past the outpost, but there can be some more soldiers wandering around.", "RDon't worry and wait here. I'll find him in no time."}, {"ADoctor Miller? We're here to help you!", "MHelp?", "RYeah, to get you out of this prison and out of this country.", "MAh, prison. Yes, yes. What's your plan?", "RAnna?", "AUmm...", "MYou have came to rescue me without any plans??", "AWe didn't know you were going to be here.", "MMy goodness! Listen to me then: the only way to get out of here safely is by taking the train back out of here. They don't seem to care about guarding out bound trains from here.", "RHow do you know?", "MObservation young man, observation. There is no science without it.", "AOK lets make a move quickly.", "MPerhaps your big friend could find some explosives if we want to make sure no one comes after us?", "AGood idea, we were about to destroy this place anyway. Go, Reed, we'll meet at the train yard.", "RSure?", "AGo, go. We can't stick around here forever."}, {"RAnna! What happened?!", "AI... I should have known that...", "RWhy oh why... don't talk too much.", "AMiller... He wasn't kidnapped... at all...", "RWhat? What are you saying?", "AIt was a trap... I don't know how to tell you but Miller is one of them. He came to Germany on his own accord?", "RMiller is a Nazi?", "AYes, he lured me... into this deceitful trap...", "RHe'll pay for that!", "ANo! You have to finish your mission. Set the dynamite... lets get the hell out of here...", "RNo, I won't leave it like that. Just... Anna?", "A...", "RHe'll pay. He'll pay good."}, {"RSo, Fort Weissberg ended up being the biggest firework I've ever seen. Soon I will board this train and head for Switzerland. I will cross the Alps by foot and, play hide and seek with German soldiers before I get there, but that's another story. Works of Clint Miller lie buried deep in the heart of the Weissberg mountain, and of course the Nazis will never finish their sonic super weapon. Miller's ties to Third Reich were never be revealed and his mysterious disappearance is still a base for numerous theories and speculations. And I? I remain on service."}};
   private int var_4db;
   private int smallFontCharsPerRow;
   private int var_550;
   private int var_59b;
   private int[] var_5fa;
   private int[] var_65d;
   private int[] var_691;
   private int[] var_6a8;
   private int var_6d3;
   private int var_75f;
   private long frameDeltaTime;
   private long accumulatedTime;
   private long lastFrameTime;
   private int frameCounter;
   private Image statusBarImage;
   private Image[] weaponSprites;
   private boolean isWeaponCentered;
   private int weaponAnimationState;
   public static int weaponSpriteFrame = 0;
   private Image crosshairImage;
   private Image largeFontImage;
   private Image smallFontImage;
   private GameObject[] cachedStaticObjects;
   private GameObject[] nextLevelObjects;
   private int[] var_b23;
   private int[] var_b56;
   private int[] var_b60;
   private int[] var_b96;
   private int var_be4;
   private int var_c1f;
   private int var_c2f;
   private int enemyUpdateCounter;
   private int enemySpawnTimer;
   private int activeEnemyCount;
   private int var_d2a;
   private int var_d88;
   private int var_d9b;
   public static byte soundEnabled = 1;
   public static byte musicEnabled = 1;
   public static byte vibrationEnabled = 1;
   public static byte gameProgressFlags = 0;
   public static byte[][] saveData;
   public static boolean mapEnabled = false;
   public static final int[] var_f4a = new int[]{5, 5, 5};
   public static final int[] var_f5c = new int[]{25, 25, 25};
   public static final int[] var_f76 = new int[]{30, 30, 30};
   public static final int[] var_fa7 = new int[]{25, 25, 25};
   public static final int[] var_ff1 = new int[]{25, 25, 25};
   public static final int[] var_104c = new int[]{100, 100, 100};
   public static final int[] var_1071 = new int[]{150, 150, 150};
   public static final int[] var_10c4 = new int[]{65536, 65536, 65536};
   public static final int[] var_1113 = new int[]{400, 400, 400};
   public static final int[] var_111e = new int[]{4, 4, 4};
   public static final int[] var_1128 = new int[]{6, 6, 6};
   public static final int[] var_1147 = new int[]{8, 8, 8};
   public static final int[] var_119b = new int[]{3, 3, 3};
   public static final int[] var_11e0 = new int[]{2, 2, 2};
   public static final int[] var_11f1 = new int[]{10, 10, 10};
   public static final int[] enemyDamageEasy = new int[]{10, 15, 20};
   public static final int[] enemyDamageNormal = new int[]{15, 20, 25};
   public static final int[] enemyDamageHard = new int[]{20, 25, 30};
   public static final int[] var_12d2 = new int[]{25, 30, 40};
   public static final int[] var_1329 = new int[]{1, 2, 3};
   public static final int[] var_1358 = new int[]{2, 4, 5};
   public static final int[] var_1366 = new int[]{3, 5, 7};
   public static final int[] var_139c = new int[]{50, 100, 150};
   public static final int[] var_13e4 = new int[]{100, 200, 300};
   public static final int[] var_13fe = new int[]{100, 200, 300};
   public static final int[] var_143e = new int[]{100, 200, 300};
   public static final int[] var_146a = new int[]{200, 400, 600};
   public static final int[] var_1492 = new int[]{300, 600, 900};
   public static final int[] var_14be = new int[]{10, 10, 10};
   public static final int[] var_14f5 = new int[]{10, 10, 10};
   public static final int[] var_151d = new int[]{1, 1, 1};
   public static final int[] var_156b = new int[]{6, 6, 6};
   public static final int[] var_157a = new int[]{10, 10, 10};
   public static final int[] var_1592 = new int[]{10, 10, 10};
   public static final int[] var_15c4 = new int[]{20, 20, 20};
   public static final int[] var_15d0 = new int[]{20, 20, 20};
   public static final int[] var_1616 = new int[]{3, 3, 3};
   public static final int[] var_1630 = new int[]{1, 1, 1};
   public static final int[] var_1677 = new int[]{3, 3, 3};
   public static final int[] var_16c7 = new int[]{25, 25, 25};
   public static final int[] var_16e8 = new int[]{50, 50, 50};
   public static final int[] var_1731 = new int[]{25, 25, 25};
   public static final int[] enemyReactionTime = new int[]{64, 64, 64};
   public static final int[] var_17b5 = new int[]{6, 4, 2};
   public static final int[] var_180b = new int[]{32, 22, 12};
   public static final int[] var_1851 = new int[]{32, 22, 12};
   public static final int[] var_18a0 = new int[]{256, 192, 128};
   public static final int[] var_18ad = new int[]{128, 128, 128};
   public static final int[] var_1910 = new int[]{128, 64, 32};
   public static final int[] var_191e = new int[]{32, 32, 32};
   public static final int[] var_1966 = new int[]{131072, 196608, 262144};
   public static final int[] var_19bb = new int[]{131072, 196608, 262144};
   public static final int[] var_19fd = new int[]{196608, 262144, 327680};
   public static final int[] var_1a1b = new int[]{196608, 262144, 327680};
   public static final int[] var_1a4a = new int[]{196608, 262144, 327680};
   public static final int[] var_1a74 = new int[]{196608, 262144, 327680};
   public static final int[] var_1ad2 = new int[]{4, 3, 2};

   public MainGameCanvas() {
      super(false);
      System.currentTimeMillis();
      this.var_4db = 18;
      this.smallFontCharsPerRow = 26;
      this.var_550 = 23;
      this.var_59b = 4;
      this.var_5fa = new int[]{1, 11, 22, 31, 42, 52, 62, 70, 82, 91, 101, 112, 120, 130, 142, 151, 161, 171, 2, 12, 20, 31, 40, 51, 61, 72, 80, 90, 100, 110, 120, 130, 142, 151, 160, 170, 1, 12, 21, 31, 41, 51, 61, 71, 81, 91, 100, 110, 120, 130, 140, 150, 160, 170};
      this.var_65d = new int[]{9, 9, 7, 8, 7, 7, 7, 10, 6, 6, 9, 6, 10, 10, 7, 9, 8, 8, 7, 6, 10, 8, 10, 9, 8, 7, 4, 4, 4, 8, 4, 4, 7, 4, 0, 0, 8, 6, 8, 8, 9, 8, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0};
      this.var_691 = new int[]{0, 8, 14, 21, 29, 36, 42, 49, 58, 64, 71, 78, 85, 91, 98, 106, 112, 120, 126, 134, 140, 148, 154, 162, 169, 176, 1, 8, 15, 22, 29, 36, 43, 50, 59, 65, 71, 80, 84, 92, 99, 106, 113, 121, 127, 135, 141, 148, 155, 162, 169, 177, 1, 9, 15, 22, 29, 36, 43, 50, 57, 64, 71, 77, 85, 92, 99, 105, 112, 121, 127, 133, 140, 147, 154, 161, 168, 175};
      this.var_6a8 = new int[]{6, 5, 6, 6, 5, 5, 6, 6, 3, 3, 5, 4, 5, 6, 6, 5, 6, 5, 5, 5, 6, 5, 7, 5, 5, 5, 5, 5, 5, 5, 5, 4, 5, 5, 1, 2, 5, 2, 7, 5, 5, 5, 5, 3, 5, 2, 5, 5, 5, 4, 5, 3, 4, 3, 4, 4, 5, 4, 4, 4, 4, 4, 1, 2, 1, 4, 1, 2, 4, 3, 2, 7, 0, 0, 0, 0, 0, 0};
      this.var_6d3 = 10;
      this.var_75f = 3;
      this.frameDeltaTime = 0L;
      this.accumulatedTime = 0L;
      this.lastFrameTime = 0L;
      this.frameCounter = 0;
      this.weaponSprites = new Image[3];
      this.isWeaponCentered = true;
      this.weaponAnimationState = 0;
      this.cachedStaticObjects = null;
      this.nextLevelObjects = null;
      this.var_b23 = null;
      this.var_b56 = null;
      this.var_b60 = null;
      this.var_b96 = null;
      this.var_be4 = 0;
      this.var_c1f = 0;
      this.var_c2f = 0;
      this.enemyUpdateCounter = 0;
      this.enemySpawnTimer = 0;
      this.activeEnemyCount = 0;
      this.var_d2a = 0;
      this.var_d88 = 0;
      this.var_d9b = 0;
      keyMappingOffset = Math.abs(this.getKeyCode(8)) == 53 ? 5 : Math.abs(this.getKeyCode(8));
      this.setFullScreenMode(true);
   }

   public void sizeChanged(int var1, int var2) {
   }

   private int translateKeyCode(int var1) {
      switch((var1 < 0 ? -var1 : var1) - keyMappingOffset) {
      case 1:
         return 11;
      case 2:
         return 12;
      default:
         switch(this.getGameAction(var1)) {
         case 1:
            return 1;
         case 2:
            return 3;
         case 3:
         case 4:
         case 7:
         default:
            return 10;
         case 5:
            return 4;
         case 6:
            return 2;
         case 8:
            return 5;
         case 9:
            return 6;
         case 10:
            return 7;
         case 11:
            return 8;
         case 12:
            return 9;
         }
      }
   }

   public void keyPressed(int var1) {
      switch(this.translateKeyCode(var1)) {
      case 1:
         GameEngine.inputForward = true;
         return;
      case 2:
         GameEngine.inputBackward = true;
         return;
      case 3:
         GameEngine.inputLookUp = true;
         return;
      case 4:
         GameEngine.inputLookDown = true;
         return;
      case 5:
         GameEngine.inputFire = true;
         GameEngine.inputStrafe = false;
         return;
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      default:
         switch(var1) {
         case 48:
            GameEngine.toggleMapInput = true;
            return;
         case 49:
            GameEngine.useKey = true;
            return;
         case 51:
            GameEngine.selectNextWeapon = true;
            return;
         case 53:
            GameEngine.inputFire = true;
            GameEngine.inputStrafe = false;
            return;
         case 55:
            GameEngine.inputLeft = true;
            return;
         case 57:
            GameEngine.inputRight = true;
         case 50:
         case 52:
         case 54:
         case 56:
         default:
            return;
         }
      case 11:
         GameEngine.inputRun = true;
         return;
      case 12:
         GameEngine.inputBack = true;
      }
   }

   public void keyReleased(int var1) {
      switch(this.translateKeyCode(var1)) {
      case 1:
         GameEngine.inputForward = false;
         return;
      case 2:
         GameEngine.inputBackward = false;
         return;
      case 3:
         GameEngine.inputLookUp = false;
         return;
      case 4:
         GameEngine.inputLookDown = false;
         return;
      case 5:
         GameEngine.inputStrafe = true;
         return;
      default:
         switch(var1) {
         case 55:
            GameEngine.inputLeft = false;
            return;
         case 57:
            GameEngine.inputRight = false;
         default:
         }
      }
   }

   private void sub_47(Graphics var1) {
      try {
         int var2 = GameEngine.renderFrame(var1, this.frameCounter) >> 15;
         int var3 = this.weaponSprites[weaponSpriteFrame].getHeight();
         int var4;
         if (GameEngine.weaponSwitchAnimationActive) {
            if ((var4 = GameEngine.weaponAnimationState) < 0) {
               var4 = -var4;
            }

            var3 = var3 * var4 >> 3;
         }

         Graphics var10000;
         Image var10001;
         int var10002;
         if (this.isWeaponCentered) {
            var10000 = var1;
            var10001 = this.weaponSprites[weaponSpriteFrame];
            var10002 = (240 - this.weaponSprites[weaponSpriteFrame].getWidth()) / 2;
         } else {
            var10000 = var1;
            var10001 = this.weaponSprites[weaponSpriteFrame];
            var10002 = 240 - this.weaponSprites[weaponSpriteFrame].getWidth();
         }

         var10000.drawImage(var10001, var10002, 288 - var3 - var2 + 3, 0);
         weaponSpriteFrame = this.weaponAnimationState;
         var1.drawImage(this.statusBarImage, 0, 288, 0);
         this.sub_547(GameEngine.playerHealth, var1, 58, 294);
         this.sub_547(GameEngine.playerArmor, var1, 138, 294);
         var4 = GameEngine.currentWeapon != 3 && GameEngine.currentWeapon != 4 ? GameEngine.currentWeapon : 1;
         this.sub_547(GameEngine.ammoCounts[var4], var1, 218, 294);
         if (GameEngine.currentWeapon > 0 && GameEngine.messageTimer == 0 && !mapEnabled) {
            var1.drawImage(this.crosshairImage, 240 - this.crosshairImage.getWidth() >> 1, 288 - this.crosshairImage.getHeight() >> 1, 0);
         }

         if (mapEnabled) {
            var1.setClip(0, 0, 240, 288);
            GameEngine.gameWorld.drawMapOnScreen(var1);
            var1.setClip(0, 0, 240, 320);
         }

      } catch (Exception var9) {
      } catch (OutOfMemoryError var10) {
      } finally {
         ;
      }
   }

   public final void startGameThread() {
      Thread var1 = new Thread(this);
      this.isGameRunning = true;
      this.isGameInitialized = false;
      var1.start();
   }

   public void run() {
      audioManager = new AudioManager();
      audioManager.loadSound("/gamedata/sound/0.mid");
      audioManager.loadSound("/gamedata/sound/1.amr");
      audioManager.loadSound("/gamedata/sound/2.amr");
      audioManager.loadSound("/gamedata/sound/3.amr");
      audioManager.loadSound("/gamedata/sound/4.amr");
      audioManager.loadSound("/gamedata/sound/5.amr");
      audioManager.loadSound("/gamedata/sound/6.amr");
      audioManager.loadSound("/gamedata/sound/7.amr");
      audioManager.loadSound("/gamedata/sound/8.amr");
      audioManager.loadSound("/gamedata/sound/9.amr");
      Graphics var1;
      (var1 = this.getGraphics()).setClip(0, 0, 240, 320);
      this.sub_175(var1);
      this.initializeGameResources();
      loadSaveData();
      loadSettingsFromRMS();
      this.areResourcesLoaded = true;
      int var2 = this.showMenuScreen(var1, true);

      label182:
      while(true) {
         while(true) {
            do {
               if (!this.isGameRunning) {
                  this.isGameInitialized = true;
                  return;
               }
            } while(this.isGamePaused);

            if (var2 == 4) {
               this.isGameInitialized = true;
               CovertOps3D.exitApplication();
               return;
            }

            GameEngine.resetPlayerProgress();
            if (var2 == 66) {
               currentLevelId = 0;
               previousLevelId = -1;
               if ((var2 = this.drawDialogOverlay(var1, 0)) != -1) {
                  continue;
               }

               this.drawPleaseWait(var1);
               this.loadLevelResources();
               break;
            }

            int[] var3 = new int[]{2, 4, 20, 5, 6, 22, 7, 9};
            int var4 = var2 - 67;
            currentLevelId = var3[var4];
            previousLevelId = -1;
            loadGameState(var4);
            GameEngine.levelTransitionState = 1;
            break;
         }

         this.accumulatedTime = 0L;
         this.lastFrameTime = 0L;

         while(this.isGameRunning) {
            try {
               if ((GameEngine.inputRun || GameEngine.inputBack || this.isGamePaused) && (var2 = this.showMenuScreen(var1, false)) != 32) {
                  break;
               }

               label170: {
                  MainGameCanvas var10000;
                  if (GameEngine.levelTransitionState == 1) {
                     switch(currentLevelId) {
                     case 0:
                     case 13:
                        currentLevelId = 0;
                        saveGameState(8);
                        if ((var2 = this.drawDialogOverlay(var1, 9)) == -1) {
                           var2 = this.showMenuScreen(var1, true);
                        }
                        continue label182;
                     case 1:
                     case 3:
                     case 8:
                     case 10:
                     case 11:
                     case 12:
                     case 14:
                     case 15:
                     case 16:
                     case 17:
                     case 18:
                     case 19:
                     case 21:
                     default:
                        break;
                     case 2:
                        saveGameState(0);
                        if ((var2 = this.drawDialogOverlay(var1, 1)) != -1) {
                           continue label182;
                        }
                        break;
                     case 4:
                     case 20:
                        if (currentLevelId == 4) {
                           saveGameState(1);
                           if ((var2 = this.drawDialogOverlay(var1, 2)) != -1) {
                              continue label182;
                           }

                           if ((var2 = this.runMiniGameSniper(var1, 0)) == -2) {
                              this.sub_180(var1);
                              var2 = this.showMenuScreen(var1, true);
                              continue label182;
                           }

                           if (var2 != -1) {
                              continue label182;
                           }
                        } else {
                           currentLevelId = 4;
                        }

                        saveGameState(2);
                        if ((var2 = this.drawDialogOverlay(var1, 3)) != -1) {
                           continue label182;
                        }
                        break;
                     case 5:
                        saveGameState(3);
                        if ((var2 = this.drawDialogOverlay(var1, 4)) != -1) {
                           continue label182;
                        }
                        break;
                     case 6:
                     case 22:
                        if (currentLevelId == 6) {
                           saveGameState(4);
                           if ((var2 = this.drawDialogOverlay(var1, 5)) != -1) {
                              continue label182;
                           }

                           if ((var2 = this.runMiniGameSniper(var1, 1)) == -2) {
                              this.sub_180(var1);
                              var2 = this.showMenuScreen(var1, true);
                              continue label182;
                           }

                           if (var2 != -1) {
                              continue label182;
                           }
                        } else {
                           currentLevelId = 6;
                        }

                        saveGameState(5);
                        if ((var2 = this.drawDialogOverlay(var1, 6)) != -1) {
                           continue label182;
                        }
                        break;
                     case 7:
                        saveGameState(6);
                        if ((var2 = this.drawDialogOverlay(var1, 7)) != -1) {
                           continue label182;
                        }
                        break;
                     case 9:
                        saveGameState(7);
                        if ((var2 = this.drawDialogOverlay(var1, 8)) != -1) {
                           continue label182;
                        }
                     }

                     var10000 = this;
                  } else {
                     if (GameEngine.levelTransitionState != -1) {
                        break label170;
                     }

                     var10000 = this;
                  }

                  var10000.drawPleaseWait(var1);
                  this.loadLevelResources();
               }

               long currentTime = System.currentTimeMillis();
               this.frameDeltaTime = currentTime - this.lastFrameTime;
               this.lastFrameTime = currentTime;
               this.accumulatedTime += this.frameDeltaTime;
               if (this.accumulatedTime > 600L) {
                  this.accumulatedTime = 600L;
               }

               while(this.accumulatedTime >= 80L) {
                  ++this.frameCounter;
                  if (this.gameLoopTick()) {
                     GameEngine.damageFlash = false;
                     this.sub_47(var1);
                     this.flushScreenBuffer();
                     this.sub_180(var1);
                     var2 = this.showMenuScreen(var1, true);
                     continue label182;
                  }

                  this.accumulatedTime -= 80L;
               }

               this.sub_47(var1);
               if (GameEngine.messageTimer > 0) {
                  this.sub_2e3(var1, GameEngine.messageText);
               }

               this.flushScreenBuffer();
               yieldToOtherThreads();
            } catch (Exception var5) {
            } catch (OutOfMemoryError var6) {
            }
         }
      }
   }

   private static int[] sub_d3(String var0, boolean var1) {
      int[] var2 = null;

      try {
         InputStream var3 = (new Object()).getClass().getResourceAsStream(var0);
         DataInputStream var4;
         (var4 = new DataInputStream(var3)).skipBytes(1);
         byte var5 = var4.readByte();
         short var6 = var4.readShort();
         short var7 = var4.readShort();
         short var8 = var4.readShort();
         int var9;
         byte[] var10 = new byte[var9 = var6 * var7];
         int var11;
         byte[] var12 = new byte[var11 = var4.readInt()];
         var4.readFully(var12, 0, var11);
         GameEngine.decompressSprite(var12, 0, var10, 0, var9, var5);
         int[] var13 = new int[var8];

         int var14;
         int var15;
         for(var14 = 0; var14 < var8; ++var14) {
            var15 = var4.readByte() & 255;
            int var16 = var4.readByte() & 255;
            int var17 = var4.readByte() & 255;
            var13[var14] = var15 << 16 | var16 << 8 | var17;
         }

         var4.close();
         var2 = new int[var9];
         if (var1) {
            for(var14 = 0; var14 < var7; ++var14) {
               for(var15 = 0; var15 < var6; ++var15) {
                  var2[var14 * var6 + (var6 - var15 - 1)] = var13[var10[var14 * var6 + var15] & 255];
               }
            }
         } else {
            for(var14 = 0; var14 < var9; ++var14) {
               var2[var14] = var13[var10[var14] & 255];
            }
         }
      } catch (Exception var18) {
      } catch (OutOfMemoryError var19) {
      }

      return var2;
   }

   private void sub_10f(int var1, int var2, int var3, byte[] var4, byte[] var5, byte[] var6) {
      try {
         String var8 = "/" + (var1 == 0 ? "gamedata/sniperminigame/ss1" : "gamedata/sniperminigame/ss2");
         InputStream var9 = (new Object()).getClass().getResourceAsStream(var8);
         DataInputStream var10;
         (var10 = new DataInputStream(var9)).skipBytes(1);
         byte var11 = var10.readByte();
         short var12 = var10.readShort();
         short var13 = var10.readShort();
         if (var12 == var2 && var13 == var3) {
            short var14 = var10.readShort();
            int var15 = var2 * var3;
            int var16;
            byte[] var17 = new byte[var16 = var10.readInt()];
            var10.readFully(var17, 0, var16);
            GameEngine.decompressSprite(var17, 0, var4, 0, var15, var11);
            this.var_b23 = new int[var14];
            this.var_b56 = new int[var14];
            this.var_b60 = new int[var14];
            this.var_b96 = new int[var14];

            int var18;
            for(var18 = 0; var18 < var14; ++var18) {
               int var19 = var10.readByte() & 255;
               int var20 = var10.readByte() & 255;
               int var21 = var10.readByte() & 255;
               this.var_b23[var18] = var19 << 16 | var20 << 8 | var21;
               this.var_b60[var18] = this.var_b23[var18] | 16711680;
               int var22;
               if ((var22 = (var22 = (var19 + var20 + var21) / 3) + (96 - (var22 >> 2))) > 255) {
                  var22 = 255;
               }

               this.var_b56[var18] = var22 << 16 | var22 << 8 | var22;
               this.var_b96[var18] = this.var_b56[var18] | 16711680;
            }

            var10.close();
            var9 = (new Object()).getClass().getResourceAsStream("/gamedata/sniperminigame/sight");
            (var10 = new DataInputStream(var9)).skipBytes(8);
            var17 = new byte[var16 = var10.readInt()];
            var10.readFully(var17, 0, var16);
            var10.close();
            GameEngine.decompressSprite(var17, 0, var6, 0, 4096, 1);
            var9 = (new Object()).getClass().getResourceAsStream(var8 + "_mask");
            (var10 = new DataInputStream(var9)).skipBytes(8);
            var17 = new byte[var16 = var10.readInt()];
            var10.readFully(var17, 0, var16);
            var10.close();
            GameEngine.decompressSprite(var17, 0, var5, 0, var15, 1);

            for(var18 = 0; var18 < var15; ++var18) {
               var5[var18] = var5[var18] == 0 ? -1 : var4[var18];
            }

         } else {
            throw new IllegalStateException();
         }
      } catch (Exception var23) {
      } catch (OutOfMemoryError var24) {
      }
   }

   private static void sub_159(Object var0, int var1, int var2) {
      int var10000 = 1;

      while(true) {
         int var3 = var10000;
         if (var10000 >= var2) {
            return;
         }

         System.arraycopy(var0, var1, var0, var1 + var3, var2 - var3 > var3 ? var3 : var2 - var3);
         var10000 = var3 + var3;
      }
   }

   private void sub_175(Graphics var1) {
      try {
         Image var2 = Image.createImage("/gamedata/sprites/logo.png");
         Image var3 = Image.createImage("/gamedata/sprites/splash.png");
         int var4;
         int[] var5 = new int[var4 = var2.getWidth() * var2.getHeight()];
         int var6 = (240 - var2.getWidth()) / 2;
         int var7 = (320 - var2.getHeight()) / 2;
         var1.setColor(16777215);
         var1.drawRect(0, 0, 240, 320);
         this.flushScreenBuffer();
         long var8 = System.currentTimeMillis();

         while(true) {
            int var10 = 16777215;
            int var11;
            if ((var11 = (int)(System.currentTimeMillis() - var8 >> 2)) < 256) {
               var10 |= 255 - var11 << 24;
            } else if (var11 >= 512 && var11 < 768) {
               var10 |= var11 - 512 << 24;
            } else if (var11 >= 768) {
               var5 = new int[var4 = var3.getWidth() * var3.getHeight()];
               var8 = System.currentTimeMillis();

               while(true) {
                  var10 = 16777215;
                  if ((var11 = (int)(System.currentTimeMillis() - var8 >> 2)) < 256) {
                     var10 |= 255 - var11 << 24;
                  } else if (var11 >= 768) {
                     return;
                  }

                  var5[0] = var10;
                  sub_159(var5, 0, var4);
                  var1.drawImage(var3, 0, 0, 20);
                  var1.drawRGB(var5, 0, var3.getWidth(), 0, 0, var3.getWidth(), var3.getHeight(), true);
                  this.flushScreenBuffer();
                  yieldToOtherThreads();
               }
            }

            var5[0] = var10;
            sub_159(var5, 0, var4);
            var1.drawImage(var2, var6, var7, 20);
            var1.drawRGB(var5, 0, var2.getWidth(), var6, var7, var2.getWidth(), var2.getHeight(), true);
            this.flushScreenBuffer();
            yieldToOtherThreads();
         }
      } catch (Exception var12) {
      } catch (OutOfMemoryError var13) {
      }
   }

   private void sub_180(Graphics var1) {
      try {
         Image var2 = Image.createImage("/gamedata/sprites/splash.png");
         GameEngine.screenBuffer[0] = -2130771968;
         sub_159(GameEngine.screenBuffer, 0, 38400);
         var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 160, true);
         var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 160, 240, 160, true);
         String var3 = "mission failed|game over";
         this.sub_2e3(var1, "mission failed|game over");
         this.flushScreenBuffer();
         delay(2000);
         long var4 = System.currentTimeMillis();

         while(true) {
            int var6 = 16711680;
            int var7;
            if ((var7 = (int)(System.currentTimeMillis() - var4 >> 4)) < 128) {
               var6 |= 255 - var7 << 24;
            } else {
               var6 |= Integer.MIN_VALUE;
               if (var7 >= 512) {
                  break;
               }
            }

            GameEngine.screenBuffer[0] = var6;
            sub_159(GameEngine.screenBuffer, 0, 38400);
            var1.drawImage(var2, 0, 0, 20);
            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 160, true);
            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 160, 240, 160, true);
            this.sub_2e3(var1, var3);
            this.flushScreenBuffer();
            yieldToOtherThreads();
         }
      } catch (Exception var8) {
         return;
      } catch (OutOfMemoryError var9) {
      }

   }

   private void sub_1e3(Graphics var1, Image var2) {
      this.accumulatedTime = 0L;
      this.lastFrameTime = System.currentTimeMillis();
      int var4 = 0;

      do {
         long var5 = System.currentTimeMillis();
         this.frameDeltaTime = var5 - this.lastFrameTime;
         this.lastFrameTime = var5;
         this.accumulatedTime += this.frameDeltaTime;
         if (this.accumulatedTime > 600L) {
            this.accumulatedTime = 600L;
         }

         while(this.accumulatedTime >= (long)6) {
            ++var4;
            this.accumulatedTime -= (long)6;
         }

         int var7 = var4;
         if (var4 > 320) {
            var7 = 320;
         }

         int var8 = 0;

         for(int var9 = 0; var9 < 240; var9 += 10) {
            Graphics var10000;
            Image var10001;
            int var10002;
            int var10003;
            byte var10004;
            int var10005;
            byte var10006;
            int var10007;
            int var10008;
            if ((var8 & 1) == 0) {
               var10000 = var1;
               var10001 = var2;
               var10002 = var9;
               var10003 = 320 - var7;
               var10004 = 10;
               var10005 = var7;
               var10006 = 0;
               var10007 = var9;
               var10008 = 0;
            } else {
               var10000 = var1;
               var10001 = var2;
               var10002 = var9;
               var10003 = 0;
               var10004 = 10;
               var10005 = var7;
               var10006 = 0;
               var10007 = var9;
               var10008 = 320 - var7;
            }

            var10000.drawRegion(var10001, var10002, var10003, var10004, var10005, var10006, var10007, var10008, 20);
            ++var8;
         }

         this.flushScreenBuffer();
      } while(var4 <= 320);

   }

   private int showMenuScreen(Graphics var1, boolean var2) {
      try {
         GameEngine.inputRun = false;
         GameEngine.inputBack = false;
         GameEngine.inputFire = false;
         GameEngine.inputForward = false;
         GameEngine.inputBackward = false;
         Image var3 = Image.createImage("/gamedata/sprites/bkg.png");
         int var4 = 0;
         int var5 = 0;
         String[] var6 = mainMenuItems;
         if (!var2) {
            var4 = 32;
            var6 = pauseMenuItems;
         }

         int var7 = 0;
         int var8 = var6.length - 2;
         this.sub_1e3(var1, var3);
         if (musicEnabled == 1 && !this.isGamePaused) {
            playSound(0, true, 80, 2);
         }

         Stack var10 = new Stack();

         while(true) {
            var1.drawImage(var3, 0, 0, 20);
            int var12 = var6.length - 1;
            int var13;
            if ((var13 = var6.length - 1) > 5) {
               var13 = 5;
            }

            int var14 = 320 - var13 * this.var_550 - 3 - this.var_550;
            int var16;
            if (var12 > var13 && var5 > 0) {
               boolean var15 = false;
               var16 = var14 + 2 * this.var_550 - 2;
               var1.setColor(16115387);
               var1.fillTriangle(117, var16, 123, var16, 120, var16 - 3);
            }

            var1.setColor(7433570);

            int var17;
            for(int var21 = 0; var21 < var13; ++var21) {
               var16 = var21;
               if (var5 > 0 && var21 > 1) {
                  var16 = var21 + var5;
               }

               String var11 = var6[var16];
               var17 = (240 - this.sub_5d2(var11)) / 2;
               if ((var4 & 15) == var16) {
                  int var18 = this.var_59b * 30;
                  var1.fillRoundRect((240 - var18) / 2, var14, var18, this.var_550, 10, 10);
               }

               this.drawLargeString(var11, var1, var17, var14);
               var14 += this.var_550;
            }

            if (var12 > var13 && var5 < var12 - 5) {
               var16 = var14 + 1;
               var1.setColor(16115387);
               var1.fillTriangle(117, var16, 123, var16, 120, var16 + 3);
            }

            String var22 = var6 == this.SETTINGS_MENU_ITEMS ? "change" : (var6 == CONFIRMATION_MENU_ITEMS ? "yes" : "select");
            this.drawLargeString(var22, var1, 3, 320 - this.var_550 - 3);
            this.drawLargeString(var6[var12], var1, 240 - this.sub_5d2(var6[var12]) - 3, 320 - this.var_550 - 3);
            this.flushScreenBuffer();
            yieldToOtherThreads();
            Object[] var23 = new Object[0];
            if (GameEngine.inputRun || GameEngine.inputFire) {
               GameEngine.inputRun = false;
               GameEngine.inputFire = false;
               switch(var4) {
               case 0:
               case 33:
                  (var23 = new Object[4])[0] = var6;
                  var23[1] = new Integer(var4);
                  var23[2] = new Integer(var7);
                  var23[3] = new Integer(var8);
                  var10.push(var23);
                  var6 = difficultyMenuItems;
                  var4 = 18 + GameEngine.difficultyLevel;
                  var7 = 2;
                  var8 = var6.length - 2;
                  break;
               case 1:
               case 34:
                  this.SETTINGS_MENU_ITEMS = new String[6];
                  this.SETTINGS_MENU_ITEMS[0] = "settings";
                  this.SETTINGS_MENU_ITEMS[1] = "";
                  this.SETTINGS_MENU_ITEMS[2] = "sound: " + (soundEnabled == 1 ? "on" : "off");
                  this.SETTINGS_MENU_ITEMS[3] = "music: " + (musicEnabled == 1 ? "on" : "off");
                  this.SETTINGS_MENU_ITEMS[4] = "vibration: " + (vibrationEnabled == 1 ? "on" : "off");
                  this.SETTINGS_MENU_ITEMS[5] = "back";
                  (var23 = new Object[4])[0] = var6;
                  var23[1] = new Integer(var4);
                  var23[2] = new Integer(var7);
                  var23[3] = new Integer(var8);
                  var10.push(var23);
                  var6 = this.SETTINGS_MENU_ITEMS;
                  var4 = 50;
                  var7 = 2;
                  var8 = this.SETTINGS_MENU_ITEMS.length - 2;
                  break;
               case 2:
               case 35:
                  this.sub_24b(var1, var3, "help", HELP_MENU_ITEMS, false);
                  break;
               case 3:
               case 36:
                  this.sub_24b(var1, var3, "about", ABOUT_MENU_TEXT, true);
                  break;
               case 4:
               case 5:
               case 6:
               case 7:
               case 8:
               case 9:
               case 10:
               case 11:
               case 12:
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 21:
               case 22:
               case 23:
               case 24:
               case 25:
               case 26:
               case 27:
               case 28:
               case 29:
               case 30:
               case 31:
               case 32:
               case 37:
               case 38:
               case 39:
               case 40:
               case 41:
               case 42:
               case 43:
               case 44:
               case 45:
               case 46:
               case 47:
               case 48:
               case 49:
               case 53:
               case 54:
               case 55:
               case 56:
               case 57:
               case 58:
               case 59:
               case 60:
               case 61:
               case 62:
               case 63:
               case 64:
               case 65:
               case 75:
               case 76:
               case 77:
               case 78:
               case 79:
               default:
                  stopCurrentSound();
                  return var4;
               case 18:
               case 19:
               case 20:
                  this.chapterMenuItems = new String[CHAPTER_MENU_DATA.length];
                  this.chapterMenuItems[0] = CHAPTER_MENU_DATA[0];
                  this.chapterMenuItems[1] = CHAPTER_MENU_DATA[1];
                  this.chapterMenuItems[2] = CHAPTER_MENU_DATA[2];
                  this.chapterMenuItems[this.chapterMenuItems.length - 1] = CHAPTER_MENU_DATA[this.chapterMenuItems.length - 1];
                  (var23 = new Object[4])[0] = var6;
                  var23[1] = new Integer(var4);
                  var23[2] = new Integer(var7);
                  var23[3] = new Integer(var8);
                  var10.push(var23);
                  GameEngine.difficultyLevel = var4 - 18;
                  loadSaveData();
                  var7 = 2;
                  var8 = this.chapterMenuItems.length - 2;

                  for(var17 = 3; var17 <= var8; ++var17) {
                     String[] var10000;
                     int var10001;
                     String var10002;
                     if (saveData[var17 - 3] != null) {
                        var10000 = this.chapterMenuItems;
                        var10001 = var17;
                        var10002 = CHAPTER_MENU_DATA[var17];
                     } else {
                        var10000 = this.chapterMenuItems;
                        var10001 = var17;
                        var10002 = "unavailable";
                     }

                     var10000[var10001] = var10002;
                  }

                  var6 = this.chapterMenuItems;
                  var4 = 66;
                  break;
               case 50:
                  soundEnabled = (byte)(soundEnabled ^ 1);
                  this.SETTINGS_MENU_ITEMS[2] = "sound: " + (soundEnabled == 1 ? "on" : "off");
                  if (musicEnabled != 1) {
                     if (soundEnabled == 1) {
                        playSound(1, false, 80, 0);
                     } else {
                        stopCurrentSound();
                     }
                  }

                  saveSettingsToRMS();
                  break;
               case 51:
                  musicEnabled = (byte)(musicEnabled ^ 1);
                  this.SETTINGS_MENU_ITEMS[3] = "music: " + (musicEnabled == 1 ? "on" : "off");
                  if (musicEnabled == 1) {
                     stopCurrentSound();
                     playSound(0, true, 80, 2);
                  } else {
                     stopCurrentSound();
                  }

                  saveSettingsToRMS();
                  break;
               case 52:
                  vibrationEnabled = (byte)(vibrationEnabled ^ 1);
                  this.SETTINGS_MENU_ITEMS[4] = "vibration: " + (vibrationEnabled == 1 ? "on" : "off");
                  if (vibrationEnabled == 1) {
                     vibrateDevice(100);
                  }

                  saveSettingsToRMS();
                  break;
               case 66:
               case 67:
               case 68:
               case 69:
               case 70:
               case 71:
               case 72:
               case 73:
               case 74:
                  if (!this.chapterMenuItems[var4 - 64].equals("unavailable")) {
                     stopCurrentSound();
                     return var4;
                  }
                  break;
               case 80:
                  stopCurrentSound();
                  return 4;
               }
            }

            if (GameEngine.inputBack) {
               GameEngine.inputBack = false;
               if (var6[var6.length - 1] != "back" && var6[var6.length - 1] != "no") {
                  if (var6[var6.length - 1] == "quit") {
                     (var23 = new Object[4])[0] = var6;
                     var23[1] = new Integer(var4);
                     var23[2] = new Integer(var7);
                     var23[3] = new Integer(var8);
                     var10.push(var23);
                     var6 = CONFIRMATION_MENU_ITEMS;
                     var4 = 80;
                     var7 = 0;
                     var8 = 0;
                  }
               } else {
                   Object[] popped = (Object[])var10.pop();
                   var6 = (String[])popped[0];
                   var4 = ((Integer)popped[1]).intValue();
                   var7 = ((Integer)popped[2]).intValue();
                   var8 = ((Integer)popped[3]).intValue();
                  var5 = 0;
               }
            }

            if (GameEngine.inputForward) {
               var16 = var4 & 15;
               --var16;
               if (var16 < var7) {
                  var16 = var7;
               } else if (var16 - var5 < 2) {
                  --var5;
               }

               var4 = var4 & -16 | var16;
               GameEngine.inputForward = false;
            }

            if (GameEngine.inputBackward) {
               var16 = var4 & 15;
               ++var16;
               if (var16 > var8) {
                  var16 = var8;
               } else if (var12 > var13 && var16 - var5 > 4) {
                  ++var5;
               }

               var4 = var4 & -16 | var16;
               GameEngine.inputBackward = false;
            }
         }
      } catch (Exception var19) {
         stopCurrentSound();
         return 4;
      } catch (OutOfMemoryError var20) {
         stopCurrentSound();
         return 4;
      }
   }

   private void sub_24b(Graphics var1, Image var2, String var3, String[] var4, boolean var5) {
      GameEngine.inputRun = false;
      GameEngine.inputBack = false;
      GameEngine.inputFire = false;
      GameEngine.inputForward = false;
      GameEngine.inputBackward = false;

      try {
         String var6 = mainMidlet.getAppProperty("MIDlet-Version");
         this.smallFontImage = Image.createImage("/gamedata/sprites/font_cut.png");
         boolean var7 = true;
         int var8 = 320 - this.var_550;

         for(int var9 = 1; var9 <= 8; ++var9) {
            GameEngine.screenBuffer[0] = 16777215 | var9 * 268435456;
            sub_159(GameEngine.screenBuffer, 0, 38400);
            var1.drawImage(var2, 0, 0, 20);
            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 160, true);
            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 160, 240, 160, true);
            this.flushScreenBuffer();
            yieldToOtherThreads();
            delay(50);
         }

         long var21 = System.currentTimeMillis();

         do {
            if (var7) {
               var1.setClip(0, 0, 240, 320);
               var1.drawImage(var2, 0, 0, 20);
               var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 160, true);
               var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 160, 240, 160, true);
               String var11 = "back";
               this.drawLargeString(var11, var1, 240 - this.sub_5d2(var11) - 3, 320 - this.var_550 - 3);
               this.drawLargeString(var3, var1, (240 - this.sub_5d2(var3)) / 2, 3);
               var1.setClip(0, this.var_550 + 6, 240, 320 - 2 * this.var_550 - 12);
               int var14;
               if (var5) {
                  long var15;
                  int var17 = (int)((var15 = System.currentTimeMillis()) - var21);
                  var14 = var8;
                  int var18 = var17 / 50 + 1;
                  if ((var8 -= var18) + var4.length * (this.var_6d3 + 2) < 0) {
                     var8 = 320 - this.var_550;
                  }

                  if ((var17 = var18 * 50 - var17) > 0) {
                     delay(var17);
                  }

                  var21 = var15;
               } else {
                  var14 = (320 - (this.var_6d3 + 2) * var4.length) / 2;
               }

               for(int var23 = 0; var23 < var4.length; ++var23) {
                  String var16 = var4[var23];
                  if (var23 == 0 && var5) {
                     var16 = var16 + " " + var6;
                  }

                  this.drawSmallString(var16, var1, (240 - this.sub_5ef(var16)) / 2, var14);
                  var14 += this.var_6d3 + 2;
               }

               this.flushScreenBuffer();
            }

            var7 = var5;
            yieldToOtherThreads();
         } while(!GameEngine.inputBack);

         GameEngine.inputBack = false;
         var1.setClip(0, 0, 240, 320);

         for(int var22 = 8; var22 >= 1; --var22) {
            GameEngine.screenBuffer[0] = 16777215 | var22 * 268435456;
            sub_159(GameEngine.screenBuffer, 0, 38400);
            var1.drawImage(var2, 0, 0, 20);
            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 160, true);
            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 160, 240, 160, true);
            this.flushScreenBuffer();
            yieldToOtherThreads();
            delay(50);
         }
      } catch (Exception var19) {
      } catch (OutOfMemoryError var20) {
      }

      GameEngine.inputRun = false;
      GameEngine.inputBack = false;
      GameEngine.inputFire = false;
      GameEngine.inputForward = false;
      GameEngine.inputBackward = false;
      this.smallFontImage = null;
      var1.setClip(0, 0, 240, 320);
   }

   private boolean sub_255(int[] var1, int[] var2, int[] var3, int[] var4, int[] var5, int[] var6, int[] var7, int[] var8) {
      int var9;
      int var10;
      int var11;
      int[] var10000;
      int var10001;
      byte var10002;
      if (this.enemySpawnTimer >= 200) {
         this.enemySpawnTimer = 0;
         var9 = 0;

         for(var10 = 0; var10 < 8; ++var10) {
            if (var1[var10] == 0) {
               var4[var9++] = var10;
            }
         }

         if (this.activeEnemyCount < 20) {
            if (var9 > 0) {
               var10 = GameEngine.random.nextInt() & 1;
               var11 = var4[(GameEngine.random.nextInt() & 7) % var9];
               var2[var11] = var10;
               int var12 = GameEngine.random.nextInt() & Integer.MAX_VALUE;
               var3[var11] = var12 % var_18ad[GameEngine.difficultyLevel] + var_18a0[GameEngine.difficultyLevel];
               var5[var11] = var6[var11];
               if (var6[var11] > var7[var11]) {
                  var10000 = var1;
                  var10001 = var11;
                  var10002 = 1;
               } else {
                  var10000 = var1;
                  var10001 = var11;
                  var10002 = 5;
               }

               var10000[var10001] = var10002;
               ++this.activeEnemyCount;
            }
         } else if (var9 == 8) {
            return false;
         }
      }

      ++this.enemySpawnTimer;
      ++this.enemyUpdateCounter;

      for(var9 = 0; var9 < 8; ++var9) {
         if (this.enemyUpdateCounter % 10 == 0) {
            label182: {
               switch(var1[var9]) {
               case 1:
                  var10000 = var1;
                  var10001 = var9;
                  var10002 = 2;
                  break;
               case 2:
                  var10000 = var1;
                  var10001 = var9;
                  var10002 = 1;
                  break;
               case 3:
               case 4:
               default:
                  break label182;
               case 5:
                  var10000 = var1;
                  var10001 = var9;
                  var10002 = 6;
                  break;
               case 6:
                  var10000 = var1;
                  var10001 = var9;
                  var10002 = 5;
               }

               var10000[var10001] = var10002;
            }
         } else if (var8[var9] == 0) {
            continue;
         }

         if ((var8[var9] != 1 || this.enemyUpdateCounter % 7 == 0) && (var8[var9] != 2 || this.enemyUpdateCounter % 5 == 0)) {
            if (var1[var9] != 1 && var1[var9] != 2) {
               if (var1[var9] != 5 && var1[var9] != 6) {
                  continue;
               }

               var10 = var5[var9] + 1;
               var11 = (var6[var9] > var7[var9] ? var6 : var7)[var9];
               if (var10 > var11) {
                  var10 = var11;
                  var1[var9] = 1;
               }

               var10000 = var5;
            } else {
               var10 = var5[var9] - 1;
               var11 = (var6[var9] < var7[var9] ? var6 : var7)[var9];
               if (var10 < var11) {
                  var10 = var11;
                  var1[var9] = 5;
               }

               var10000 = var5;
            }

            var10000[var9] = var10;
         }
      }

      for(var9 = 0; var9 < 8; ++var9) {
         if ((var10 = var1[var9]) != 0) {
            int var16;
            if (var3[var9] <= 0) {
               label154: {
                  switch(var10) {
                  case 1:
                  case 2:
                  case 5:
                  case 6:
                     var1[var9] = 3;
                     var11 = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                     var10000 = var3;
                     var10001 = var9;
                     var16 = var11 % var_191e[GameEngine.difficultyLevel] + var_1910[GameEngine.difficultyLevel];
                     break;
                  case 3:
                     var1[var9] = 4;
                     var10000 = var3;
                     var10001 = var9;
                     var16 = 1;
                     break;
                  case 4:
                  default:
                     break label154;
                  }

                  var10000[var10001] = var16;
               }
            }

            var16 = var3[var9]--;
         }
      }

      int[] var13 = new int[]{0, 0, -1, 1, -1, 1, -1, 1};
      int[] var15 = new int[]{-1, 1, 0, 0, -1, 1, 1, -1};
      var11 = this.var_c2f;
      this.var_c2f = var11 + 1 & 7;
      this.var_d88 += var13[var11];
      this.var_d9b += var15[var11];
      byte var14 = 0;
      if (GameEngine.inputLookUp) {
         if (this.var_d2a == 3) {
            --this.var_be4;
         }

         var14 = 3;
      }

      if (GameEngine.inputLookDown) {
         if (this.var_d2a == 4) {
            ++this.var_be4;
         }

         var14 = 4;
      }

      if (GameEngine.inputForward) {
         if (this.var_d2a == 1) {
            --this.var_c1f;
         }

         var14 = 1;
      }

      if (GameEngine.inputBackward) {
         if (this.var_d2a == 2) {
            ++this.var_c1f;
         }

         var14 = 2;
      }

      this.var_d88 += this.var_be4 >> 2;
      this.var_d9b += this.var_c1f >> 2;
      if (this.var_d88 > 208) {
         this.var_d88 = 208;
         this.var_be4 = 0;
      }

      if (this.var_d88 < -31) {
         this.var_d88 = -31;
         this.var_be4 = 0;
      }

      if (this.var_d9b > 256) {
         this.var_d9b = 256;
         this.var_c1f = 0;
      }

      if (this.var_d9b < -31) {
         this.var_d9b = -31;
         this.var_c1f = 0;
      }

      this.var_d2a = var14;
      if (this.var_d2a == 0) {
         if (this.var_be4 > 0) {
            --this.var_be4;
         }

         if (this.var_be4 < 0) {
            ++this.var_be4;
         }

         if (this.var_c1f > 0) {
            --this.var_c1f;
         }

         if (this.var_c1f < 0) {
            ++this.var_c1f;
         }
      }

      return true;
   }

   private int runMiniGameSniper(Graphics var1, int var2) {
      try {
         boolean var3 = false;
         int var5;
         byte[] var7 = new byte[var5 = 240 * 288];
         byte[] var8 = new byte[var5];
         byte[] var9 = new byte[4096];
         int[][] var10 = new int[6][];
         int[][] var11 = new int[6][];
         int[][] var12 = new int[6][];
         int[][] var13 = new int[6][];
         this.enemySpawnTimer = 0;
         this.enemyUpdateCounter = 0;
         this.activeEnemyCount = 0;
         int[][] var14 = new int[][]{{84, 147, 197, 132, 147, 155, 77, 155}, {63, 177, 89, 149, 104, 132, 84, 146}};
         int[][] var15 = new int[][]{{147, 84, 164, 147, 132, 160, 155, 77}, {75, 162, 149, 89, 132, 104, 90, 152}};
         int[][] var16 = new int[][]{{145, 145, 102, 84, 84, 144, 151, 151}, {108, 105, 111, 111, 160, 160, 152, 152}};
         int[][] var17 = new int[][]{{1, 1, 1, 0, 0, 2, 2, 2}, {1, 1, 1, 1, 1, 1, 1, 1}};
         int[] var18 = new int[]{4, 9, 14};
         int[] var19 = new int[]{8, 18, 30};
         int[] var20 = new int[]{var_1329[GameEngine.difficultyLevel], var_1358[GameEngine.difficultyLevel], var_1366[GameEngine.difficultyLevel]};
         boolean var21 = false;
         int[] var22 = new int[]{0, 0};
         int[] var23 = new int[]{0, 0};
         int[][] var24 = new int[8][];
         int[] var25 = new int[8];
         int[] var26 = new int[8];
         int[] var27 = new int[8];
         int[] var28 = new int[8];

         int var29;
         for(var29 = 0; var29 < 8; ++var29) {
            var14[var2][var29] -= var22[var2];
            var15[var2][var29] -= var22[var2];
            var16[var2][var29] -= var23[var2];
         }

         int var31;
         for(var29 = 0; var29 < 6; ++var29) {
            boolean var30 = var29 > 3;
            var31 = var29 > 3 ? var29 - 3 : var29 + 1;
            var10[var29] = sub_d3("/gamedata/sniperminigame/ot8" + Integer.toString(var31), var30);
            var11[var29] = sub_d3("/gamedata/sniperminigame/ot18" + Integer.toString(var31), var30);
            var12[var29] = sub_d3("/gamedata/sniperminigame/ot30" + Integer.toString(var31), var30);
            var13[var29] = sub_d3("/gamedata/sniperminigame/ss30" + Integer.toString(var31), var30);
         }

         Image var61 = Image.createImage("/gamedata/sniperminigame/sight.png");
         this.sub_10f(var2, 240, 288, var7, var8, var9);
         this.var_be4 = 0;
         this.var_c1f = 0;
         this.var_d88 = 88;
         this.var_d9b = 112;
         int[] var32 = new int[8];
         this.accumulatedTime = 0L;
         this.lastFrameTime = 0L;
         GameEngine.levelTransitionState = 2;

         while(this.isGameRunning) {
            if (GameEngine.levelTransitionState == 1) {
               return -1;
            }

            int var34;
            if ((GameEngine.inputRun || GameEngine.inputBack || this.isGamePaused) && (var34 = this.showMenuScreen(var1, false)) != 32) {
               return var34;
            }

            long var63 = System.currentTimeMillis();
            this.frameDeltaTime = var63 - this.lastFrameTime;
            this.lastFrameTime = var63;
            this.accumulatedTime += this.frameDeltaTime;
            if (this.accumulatedTime > 600L) {
               this.accumulatedTime = 600L;
            }

            while(this.accumulatedTime >= (long)40) {
               ++this.frameCounter;
               if (!this.sub_255(var26, var25, var27, var32, var28, var14[var2], var15[var2], var17[var2])) {
                  return -1;
               }

               this.accumulatedTime -= (long)40;
            }

            int var36 = 0;

            for(int var37 = 0; var37 < 8; ++var37) {
               if (var26[var37] == 4) {
                  var36 += var20[var17[var2][var37]];
               }
            }

            int[] var64 = this.var_b56;
            int[] var38 = this.var_b23;
            if (var36 > 0) {
               byte var10000;
               boolean var10001;
               byte var10002;
               if (var36 > var_1329[GameEngine.difficultyLevel]) {
                  if (var36 > var_1358[GameEngine.difficultyLevel]) {
                     var10000 = 2;
                     var10001 = false;
                     var10002 = 100;
                  } else {
                     var10000 = 2;
                     var10001 = false;
                     var10002 = 80;
                  }
               } else {
                  var10000 = 2;
                  var10001 = false;
                  var10002 = 60;
               }

               playSound(var10000, var10001, var10002, 0);
               vibrateDevice(var36 * 10);
               if (GameEngine.applyDamage(var36)) {
                  var21 = true;
               } else {
                  var64 = this.var_b96;
                  var38 = this.var_b60;
               }
            }

            int var62 = this.var_d88;
            int var39 = (var31 = this.var_d9b) < 0 ? 0 : var31;
            int var40;
            if ((var40 = var31 + 64) > 288) {
               var40 = 288;
            }

            int var41 = var62 < 0 ? 0 : var62;
            int var42;
            if ((var42 = var62 + 64) > 240) {
               var42 = 240;
            }

            int var43;
            int var44;
            int var45;
            int var46;
            for(var43 = var39; var43 < var40; ++var43) {
               var44 = var41 + 240 * var43;
               var45 = var42 + 240 * var43;

               for(var46 = var44; var46 < var45; ++var46) {
                  GameEngine.screenBuffer[var46] = var38[var7[var46] & 255];
               }
            }

            var43 = var2 == 0 ? 6 : 8;

            int var47;
            int var48;
            int var49;
            int var50;
            int[][] var66;
            int var67;
            int[][] var68;
            for(var44 = 0; var44 < var43; ++var44) {
               if (var26[var44] > 0) {
                  label273: {
                     var24[var44] = null;
                     var45 = var28[var44];
                     var46 = var16[var2][var44];
                     var47 = var17[var2][var44];
                     var48 = var18[var47];
                     var49 = var19[var47];
                     switch(var47) {
                     case 0:
                        var66 = var24;
                        var67 = var44;
                        var68 = var10;
                        break;
                     case 1:
                        var66 = var24;
                        var67 = var44;
                        var68 = var11;
                        break;
                     case 2:
                        if (var25[var44] == 0) {
                           var66 = var24;
                           var67 = var44;
                           var68 = var12;
                        } else {
                           var66 = var24;
                           var67 = var44;
                           var68 = var13;
                        }
                        break;
                     default:
                        break label273;
                     }

                     var66[var67] = var68[var26[var44] - 1];
                  }

                  copyToScreenBuffer(var24[var44], var48, var49, var45, var46, var36 > 0);
                  if (var26[var44] == 4) {
                     var26[var44] = (var28[var44] & 1) == 1 ? 1 : 5;
                     var50 = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                     var27[var44] = var50 % var_18ad[GameEngine.difficultyLevel] + var_18a0[GameEngine.difficultyLevel];
                  }
               }
            }

            for(var44 = var39; var44 < var40; ++var44) {
               var45 = var41 + 240 * var44;
               var46 = var42 + 240 * var44;

               for(var47 = var45; var47 < var46; ++var47) {
                  if ((var48 = var8[var47] & 255) != 255) {
                     GameEngine.screenBuffer[var47] = var38[var48];
                  }
               }
            }

            if (var2 == 0) {
               for(var44 = 6; var44 < 8; ++var44) {
                  if (var26[var44] > 0) {
                     var24[var44] = null;
                     var45 = var28[var44];
                     var46 = var16[var2][var44];
                     var47 = var17[var2][var44];
                     var48 = var18[var47];
                     var49 = var19[var47];
                     if (var25[var44] == 0) {
                        var66 = var24;
                        var67 = var44;
                        var68 = var12;
                     } else {
                        var66 = var24;
                        var67 = var44;
                        var68 = var13;
                     }

                     var66[var67] = var68[var26[var44] - 1];
                     copyToScreenBuffer(var24[var44], var48, var49, var45, var46, var36 > 0);
                     if (var26[var44] == 4) {
                        var26[var44] = (var28[var44] & 1) == 1 ? 1 : 5;
                        var50 = GameEngine.random.nextInt() & Integer.MAX_VALUE;
                        var27[var44] = var50 % var_18ad[GameEngine.difficultyLevel] + var_18a0[GameEngine.difficultyLevel];
                     }
                  }
               }
            }

            var44 = 240 * var39;

            for(var45 = 0; var45 < var44; ++var45) {
               GameEngine.screenBuffer[var45] = var64[var7[var45] & 255];
            }

            var45 = var39 - var31;

            int var51;
            for(var46 = var39; var46 < var40; ++var45) {
               var44 = (var47 = 240 * var46) + var41;

               for(var48 = var47; var48 < var44; ++var48) {
                  GameEngine.screenBuffer[var48] = var64[var7[var48] & 255];
               }

               var44 = var47 + 240;

               for(var48 = var42 + var47; var48 < var44; ++var48) {
                  GameEngine.screenBuffer[var48] = var64[var7[var48] & 255];
               }

               var48 = 64 * var45;
               var49 = var41 - var62;

               for(var50 = var41; var50 < var42; ++var49) {
                  if (var9[var48 + var49] == 0) {
                     var51 = var47 + var50;
                     GameEngine.screenBuffer[var51] = var64[var7[var51] & 255];
                  }

                  ++var50;
               }

               ++var46;
            }

            for(var47 = var46 = 240 * var40; var47 < var5; ++var47) {
               GameEngine.screenBuffer[var47] = var64[var7[var47] & 255];
            }

            if (GameEngine.inputFire) {
               var47 = this.var_d88 + 32 - 1;
               var48 = this.var_d9b + 32 - 1;
               var49 = 16777215;
               boolean var65 = false;

               for(var51 = 7; var51 >= 0; --var51) {
                  int var52 = var28[var51];
                  int var53 = var16[var2][var51];
                  int var54 = var17[var2][var51];
                  int var55 = var18[var54];
                  int var56 = var19[var54];
                  if ((var2 == 0 && (var51 >= 6 || var54 == 0) || var8[var48 * 240 + var47] == -1) && var26[var51] > 0 && var47 >= var52 && var47 <= var52 + var55 && var48 >= var53 && var48 <= var53 + var56) {
                     int var57 = var47 - var52;
                     int var58 = var48 - var53;
                     if (var65 = var2 == 0 && var54 == 0 ? true : var24[var51][var55 * var58 + var57] != 16711935) {
                        playSound(7, false, 100, 1);
                        var26[var51] = 0;
                        var49 = 16711680;
                        break;
                     }
                  }
               }

               if (!var65) {
                  playSound((GameEngine.random.nextInt() & 1) == 0 ? 2 : 6, false, 100, 1);
               }

               GameEngine.screenBuffer[240 * var48 + var47] = var49;
               GameEngine.inputFire = false;
            }

            var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 288, false);
            var1.drawImage(var61, var62, var31, 20);
            var1.drawImage(this.statusBarImage, 0, 288, 0);
            this.sub_547(GameEngine.playerHealth, var1, 58, 294);
            this.sub_547(GameEngine.playerArmor, var1, 138, 294);
            this.flushScreenBuffer();
            if (var21) {
               return -2;
            }

            yieldToOtherThreads();
         }
      } catch (Exception var59) {
      } catch (OutOfMemoryError var60) {
      }

      return -1;
   }

   private static void copyToScreenBuffer(int[] var0, int var1, int var2, int var3, int var4, boolean var5) {
      int var6 = 240 * var4 + var3;
      int var7 = 0;
      int var8;
      int var9;
      int var10;
      if (!var5) {
         for(var8 = 0; var8 < var2; ++var8) {
            for(var9 = 0; var9 < var1; ++var9) {
               if ((var10 = var0[var7++]) != 16711935) {
                  GameEngine.screenBuffer[var6 + var9] = var10;
               }
            }

            var6 += 240;
         }

      } else {
         for(var8 = 0; var8 < var2; ++var8) {
            for(var9 = 0; var9 < var1; ++var9) {
               if ((var10 = var0[var7++]) != 16711935) {
                  GameEngine.screenBuffer[var6 + var9] = var10 | 16711680;
               }
            }

            var6 += 240;
         }

      }
   }

   private void sub_2e3(Graphics var1, String var2) {
      int var3 = 0;
      int var5 = 0;

      int var4;
      do {
         if ((var4 = var2.indexOf(124, var3)) == -1) {
            var4 = var2.length() - 1;
         } else {
            --var4;
         }

         ++var5;
      } while((var3 = var4 + 2) < var2.length());

      int var7 = 160 - this.var_550 * var5 / 2;
      var3 = 0;

      do {
         if ((var4 = var2.indexOf(124, var3)) == -1) {
            var4 = var2.length() - 1;
         } else {
            --var4;
         }

         String var8 = var2.substring(var3, var4 + 1);
         int var6 = (240 - this.sub_5d2(var8)) / 2;
         this.drawLargeString(var8, var1, var6, var7);
         var7 += this.var_550;
      } while((var3 = var4 + 2) < var2.length());

   }

   public final void stopGame() {
      if (!this.isGamePaused) {
         this.isGamePaused = true;
         if (audioManager != null) {
            audioManager.stopCurrentSound();
         }

      }
   }

   public final void resumeGame() {
      if (this.isGamePaused) {
         if (audioManager != null && musicEnabled == 1 && this.areResourcesLoaded) {
            playSound(0, true, 80, 2);
         }

         this.isGamePaused = false;
      }
   }

   public void showNotify() {
      this.resumeGame();
   }

   public void hideNotify() {
      this.stopGame();
   }

   public final void stopGameLoop() {
      this.isGameRunning = false;
   }

   public final boolean gameLoopTick() {
      if (GameEngine.updateGameLogic()) {
         return true;
      } else {
         if (!GameEngine.weaponSwitchAnimationActive) {
            GameEngine.pendingWeaponSwitch = GameEngine.currentWeapon;
            if (GameEngine.selectNextWeapon) {
               GameEngine.selectNextWeapon = false;
               GameEngine.pendingWeaponSwitch = GameEngine.cycleWeaponForward(GameEngine.pendingWeaponSwitch);
            }

            GameEngine.pendingWeaponSwitch = GameEngine.findNextAvailableWeapon(GameEngine.pendingWeaponSwitch);
            if (GameEngine.pendingWeaponSwitch != GameEngine.currentWeapon) {
               GameEngine.weaponSwitchAnimationActive = true;
               GameEngine.weaponAnimationState = 8;
            }
         }

         if (GameEngine.weaponSwitchAnimationActive) {
            --GameEngine.weaponAnimationState;
            if (GameEngine.weaponAnimationState == -8) {
               GameEngine.weaponSwitchAnimationActive = false;
            }

            if (GameEngine.weaponAnimationState == 0) {
               GameEngine.currentWeapon = GameEngine.pendingWeaponSwitch;

               try {
                  label190: {
                     MainGameCanvas var3;
                     boolean var4;
                     label162: {
                        Image[] var10000;
                        byte var10001;
                        Image var5;
                        label161: {
                           String var10002;
                           switch(GameEngine.currentWeapon) {
                           case 0:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/fist_a.png");
                              var10000 = this.weaponSprites;
                              var10001 = 1;
                              var10002 = "/gamedata/sprites/fist_b.png";
                              break;
                           case 1:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/luger_a.png");
                              var10000 = this.weaponSprites;
                              var10001 = 1;
                              var10002 = "/gamedata/sprites/luger_b.png";
                              break;
                           case 2:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/mauser_a.png");
                              this.weaponSprites[1] = Image.createImage("/gamedata/sprites/mauser_b.png");
                              this.weaponSprites[2] = null;
                              var3 = this;
                              var4 = false;
                              break label162;
                           case 3:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/m40_a.png");
                              this.weaponSprites[1] = Image.createImage("/gamedata/sprites/m40_b.png");
                              this.weaponSprites[2] = null;
                              var3 = this;
                              var4 = false;
                              break label162;
                           case 4:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/sten_a.png");
                              this.weaponSprites[1] = Image.createImage("/gamedata/sprites/sten_b.png");
                              this.weaponSprites[2] = null;
                              var3 = this;
                              var4 = false;
                              break label162;
                           case 5:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/panzerfaust_a.png");
                              this.weaponSprites[1] = Image.createImage("/gamedata/sprites/panzerfaust_b.png");
                              this.weaponSprites[2] = Image.createImage("/gamedata/sprites/panzerfaust_c.png");
                              var3 = this;
                              var4 = false;
                              break label162;
                           case 6:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/dynamite.png");
                              var10000 = this.weaponSprites;
                              var10001 = 1;
                              var5 = null;
                              break label161;
                           case 7:
                              this.weaponSprites[0] = Image.createImage("/gamedata/sprites/sonic_a.png");
                              var10000 = this.weaponSprites;
                              var10001 = 1;
                              var10002 = "/gamedata/sprites/sonic_b.png";
                              break;
                           default:
                              break label190;
                           }

                           var5 = Image.createImage(var10002);
                        }

                        var10000[var10001] = var5;
                        this.weaponSprites[2] = null;
                        var3 = this;
                        var4 = true;
                     }

                     var3.isWeaponCentered = var4;
                  }

                  this.weaponAnimationState = 0;
                  weaponSpriteFrame = 0;
               } catch (Exception var1) {
               } catch (OutOfMemoryError var2) {
               }
            }
         }

         if (GameEngine.weaponCooldownTimer > -32768) {
            --GameEngine.weaponCooldownTimer;
         }

         if (GameEngine.inputFire && !GameEngine.weaponSwitchAnimationActive) {
            int var6;
            switch(GameEngine.currentWeapon) {
            case 0:
               if (GameEngine.weaponCooldownTimer < -var_111e[GameEngine.difficultyLevel]) {
                  GameEngine.gameWorld.fireWeapon();
                  this.weaponAnimationState = 1;
                  weaponSpriteFrame = 1;
                  GameEngine.weaponCooldownTimer = 1;
               }
               break;
            case 1:
               if (GameEngine.weaponCooldownTimer < -var_1128[GameEngine.difficultyLevel] && GameEngine.ammoCounts[GameEngine.currentWeapon] > 0) {
                  var6 = GameEngine.ammoCounts[GameEngine.currentWeapon]--;
                  GameEngine.gameWorld.fireWeapon();
                  this.weaponAnimationState = 1;
                  weaponSpriteFrame = 1;
                  GameEngine.weaponCooldownTimer = 1;
               }
               break;
            case 2:
               if (GameEngine.weaponCooldownTimer < -var_1147[GameEngine.difficultyLevel] && GameEngine.ammoCounts[GameEngine.currentWeapon] > 0) {
                  var6 = GameEngine.ammoCounts[GameEngine.currentWeapon]--;
                  GameEngine.gameWorld.fireWeapon();
                  this.weaponAnimationState = 1;
                  weaponSpriteFrame = 1;
                  GameEngine.weaponCooldownTimer = 1;
               }
               break;
            case 3:
               if (GameEngine.weaponCooldownTimer <= 0) {
                  if (this.weaponAnimationState == 0) {
                     if (GameEngine.ammoCounts[1] > 0) {
                        var6 = GameEngine.ammoCounts[1]--;
                        GameEngine.gameWorld.fireWeapon();
                        this.weaponAnimationState = 1;
                        weaponSpriteFrame = 1;
                        GameEngine.weaponCooldownTimer = 1;
                     }
                  } else {
                     this.weaponAnimationState = 0;
                     GameEngine.weaponCooldownTimer = var_119b[GameEngine.difficultyLevel];
                  }
               }
               break;
            case 4:
               if (GameEngine.weaponCooldownTimer <= 0) {
                  if (this.weaponAnimationState == 0) {
                     if (GameEngine.ammoCounts[1] > 0) {
                        var6 = GameEngine.ammoCounts[1]--;
                        GameEngine.gameWorld.fireWeapon();
                        this.weaponAnimationState = 1;
                        weaponSpriteFrame = 1;
                        GameEngine.weaponCooldownTimer = 1;
                     }
                  } else {
                     this.weaponAnimationState = 0;
                     GameEngine.weaponCooldownTimer = var_11e0[GameEngine.difficultyLevel];
                  }
               }
               break;
            case 5:
               if (GameEngine.weaponCooldownTimer <= -1 && GameEngine.ammoCounts[GameEngine.currentWeapon] > 0) {
                  var6 = GameEngine.ammoCounts[GameEngine.currentWeapon]--;
                  GameEngine.gameWorld.fireWeapon();
                  this.weaponAnimationState = 1;
                  weaponSpriteFrame = 1;
                  GameEngine.weaponCooldownTimer = 2;
               }
               break;
            case 6:
               if (GameEngine.weaponCooldownTimer <= -1 && GameEngine.ammoCounts[6] > 0) {
                  if ((currentLevelId == 4 || currentLevelId == 7 || currentLevelId == 8) && (currentLevelId != 4 || GameEngine.currentSector.getSectorType() != 666) && GameEngine.ammoCounts[6] == 1) {
                     GameEngine.messageText = "i'd better use it|to finish my mission";
                     GameEngine.messageTimer = 50;
                  } else if (GameEngine.gameWorld.throwGrenade()) {
                     var6 = GameEngine.ammoCounts[6]--;
                     GameEngine.weaponCooldownTimer = 0;
                     GameEngine.weaponAnimationState = 8;
                     GameEngine.weaponSwitchAnimationActive = true;
                     GameEngine.pendingWeaponSwitch = GameEngine.findNextAvailableWeapon(6);
                  }
               }
               break;
            case 7:
               if (GameEngine.weaponCooldownTimer < -var_11f1[GameEngine.difficultyLevel] && GameEngine.ammoCounts[GameEngine.currentWeapon] > 0) {
                  var6 = GameEngine.ammoCounts[GameEngine.currentWeapon]--;
                  GameEngine.gameWorld.fireWeapon();
                  this.weaponAnimationState = 1;
                  weaponSpriteFrame = 1;
                  GameEngine.weaponCooldownTimer = 1;
               }
            }
         } else if (GameEngine.weaponCooldownTimer <= 0) {
            if (GameEngine.currentWeapon == 5) {
               if (this.weaponAnimationState == 1) {
                  this.weaponAnimationState = 2;
                  weaponSpriteFrame = 2;
                  GameEngine.weaponAnimationState = 8;
                  GameEngine.weaponSwitchAnimationActive = true;
                  GameEngine.pendingWeaponSwitch = GameEngine.findNextAvailableWeapon(5);
               }
            } else {
               this.weaponAnimationState = 0;
            }
         }

         if (GameEngine.currentWeapon != 3 && GameEngine.currentWeapon != 4 || GameEngine.inputStrafe) {
            GameEngine.inputFire = false;
         }

         return false;
      }
   }

   private static void sub_3bc(GameObject var0, byte[] var1, byte[] var2) {
      for(int var3 = 0; var3 < var1.length; ++var3) {
         byte var4 = var1[var3];
         byte var5 = var2[var3];
         var0.addSpriteFrame(var4, var5);
         if (var4 != 0) {
            GameEngine.preloadTexture(var4);
         }

         if (var5 != 0) {
            GameEngine.preloadTexture(var5);
         }
      }

   }

   private void initializeGameResources() {
      try {
         this.statusBarImage = Image.createImage("/gamedata/sprites/bar.png");
         this.weaponSprites[0] = Image.createImage("/gamedata/sprites/fist_a.png");
         this.weaponSprites[1] = Image.createImage("/gamedata/sprites/fist_b.png");
         this.weaponSprites[2] = null;
         this.crosshairImage = Image.createImage("/gamedata/sprites/aim.png");
         this.largeFontImage = Image.createImage("/gamedata/sprites/font.png");
         MathUtils.initializeMathTables();
         GameEngine.initializeEngine();
      } catch (Exception var1) {
      } catch (OutOfMemoryError var2) {
      }
   }

   private void loadLevelResources() {
      try {
         freeMemory();
         if (previousLevelId < currentLevelId) {
            if (previousLevelId > -1) {
               this.cachedStaticObjects = GameEngine.gameWorld.staticObjects;
            }

            if (!GameEngine.loadMapData("/gamedata/levels/level_" + levelFileNames[currentLevelId], this.nextLevelObjects == null)) {
               CovertOps3D.exitApplication();
            }

            if (this.nextLevelObjects != null) {
               GameEngine.gameWorld.staticObjects = this.nextLevelObjects;
               this.nextLevelObjects = null;
            } else {
               GameEngine.keysCollected[0] = false;
               GameEngine.keysCollected[1] = false;
            }
         } else {
            this.nextLevelObjects = GameEngine.gameWorld.staticObjects;
            if (!GameEngine.loadMapData("/level_" + levelFileNames[currentLevelId], this.cachedStaticObjects == null)) {
               CovertOps3D.exitApplication();
            }

            if (this.cachedStaticObjects != null) {
               GameEngine.gameWorld.staticObjects = this.cachedStaticObjects;
               this.cachedStaticObjects = null;
            }
         }

         freeMemory();
         GameEngine.resetLevelState();
         boolean var1 = false;
         GameEngine.preloadTexture((byte)25);
         byte[] var2 = new byte[]{-23, -25, -28, -30, -32, 0, 0};
         byte[] var3 = new byte[]{-24, -26, -29, -31, -33, -34, -27};
         byte[] var4 = new byte[]{-35, -36, -38, -39, -40, 0, 0};
         byte[] var5 = new byte[]{-86, -88, -91, -93, -95, 0, 0};
         byte[] var6 = new byte[]{-87, -89, -92, -94, -96, -97, -90};
         byte[] var7 = new byte[]{-73, -75, -78, -80, -82, 0, 0};
         byte[] var8 = new byte[]{-74, -76, -79, -81, -83, -84, -77};
         byte[] var9 = new byte[]{-2, -1};
         byte[] var10 = new byte[]{-3, 0};
         byte[] var11 = new byte[]{-59, -61, -64, -66, -68, 0, 0};
         byte[] var12 = new byte[]{-60, -62, -65, -67, -69, -70, -63};
         byte[] var13 = new byte[]{-9};
         byte[] var14 = new byte[]{-10};
         byte[] var15 = new byte[]{-4, -6, -11, -13, 0, 0};
         byte[] var16 = new byte[]{-5, -7, -12, -14, -15, -8};
         GameObject[] var17 = GameEngine.gameWorld.staticObjects;

         for(int var18 = 0; var18 < var17.length; ++var18) {
            GameObject var19;
            if ((var19 = var17[var18]) != null) {
               byte var10000;
               label166: {
                  label165: {
                     GameObject var22;
                     switch(var19.objectType) {
                     case 5:
                     case 13:
                        var19.addSpriteFrame((byte)0, (byte)-53);
                        var10000 = -53;
                        break label166;
                     case 10:
                        sub_3bc(var19, var9, var10);
                        if (levelFileNames[currentLevelId] == "06c") {
                           var19.currentState = 1;
                        }
                        continue;
                     case 12:
                        sub_3bc(var19, var13, var14);
                        continue;
                     case 26:
                        var19.addSpriteFrame((byte)0, (byte)-16);
                        var10000 = -16;
                        break label166;
                     case 60:
                        var19.addSpriteFrame((byte)0, (byte)-18);
                        var10000 = -18;
                        break label166;
                     case 61:
                        var19.addSpriteFrame((byte)0, (byte)-17);
                        var10000 = -17;
                        break label166;
                     case 82:
                        var19.addSpriteFrame((byte)0, (byte)-21);
                        var10000 = -21;
                        break label166;
                     case 2001:
                        var19.addSpriteFrame((byte)0, (byte)-19);
                        var10000 = -19;
                        break label166;
                     case 2002:
                        var19.addSpriteFrame((byte)0, (byte)-20);
                        var10000 = -20;
                        break label166;
                     case 2003:
                        var19.addSpriteFrame((byte)0, (byte)-22);
                        var10000 = -22;
                        break label166;
                     case 2004:
                        var19.addSpriteFrame((byte)0, (byte)-43);
                        var10000 = -43;
                        break label166;
                     case 2005:
                        var19.addSpriteFrame((byte)0, (byte)-50);
                        var10000 = -50;
                        break label166;
                     case 2006:
                        var19.addSpriteFrame((byte)0, (byte)-72);
                        var10000 = -72;
                        break label166;
                     case 2007:
                        var22 = var19;
                        break;
                     case 2008:
                        var19.addSpriteFrame((byte)0, (byte)-54);
                        var10000 = -54;
                        break label166;
                     case 2010:
                        var19.addSpriteFrame((byte)0, (byte)-57);
                        var10000 = -57;
                        break label166;
                     case 2012:
                        var19.addSpriteFrame((byte)0, (byte)-55);
                        var10000 = -55;
                        break label166;
                     case 2013:
                        var19.addSpriteFrame((byte)0, (byte)-49);
                        var10000 = -49;
                        break label166;
                     case 2014:
                        var19.addSpriteFrame((byte)0, (byte)-52);
                        var10000 = -52;
                        break label166;
                     case 2015:
                        var19.addSpriteFrame((byte)0, (byte)-58);
                        var10000 = -58;
                        break label166;
                     case 2024:
                        var19.addSpriteFrame((byte)0, (byte)-85);
                        var10000 = -85;
                        break label166;
                     case 2047:
                        var19.addSpriteFrame((byte)0, (byte)-56);
                        var10000 = -56;
                        break label166;
                     case 3001:
                        sub_3bc(var19, var11, var12);
                        var10000 = -57;
                        break label166;
                     case 3002:
                        sub_3bc(var19, var15, var16);
                        var10000 = -56;
                        break label166;
                     case 3003:
                        sub_3bc(var19, var2, var3);
                        break label165;
                     case 3004:
                        sub_3bc(var19, var5, var6);
                        var10000 = -54;
                        break label166;
                     case 3005:
                        sub_3bc(var19, var4, var6);
                        break label165;
                     case 3006:
                        sub_3bc(var19, var7, var8);
                        var10000 = -54;
                        break label166;
                     default:
                        var22 = var19;
                     }

                     var22.addSpriteFrame((byte)0, (byte)-48);
                  }

                  var10000 = -48;
               }

               GameEngine.preloadTexture(var10000);
            }
         }

         GameEngine.preloadTexture((byte)-44);
         GameEngine.preloadTexture((byte)-45);
         GameEngine.preloadTexture((byte)-46);
         GameEngine.preloadTexture((byte)-47);
         GameEngine.preloadTexture((byte)-71);
         GameEngine.preloadTexture((byte)-51);
         GameEngine.preloadTexture((byte)-43);
         if (currentLevelId == 10) {
            GameEngine.preloadTexture((byte)-72);
         }

         freeMemory();
         if (!GameEngine.loadGameAssets("/gamedata/textures/tx", 4, "/gamedata/textures/sp", 4)) {
            CovertOps3D.exitApplication();
         }

         GameEngine.handleWeaponChange((byte)25);
         freeMemory();
      } catch (Exception var20) {
      } catch (OutOfMemoryError var21) {
      }
   }

   private void drawPleaseWait(Graphics var1) {
      String var2 = "please wait...";
      int var3 = (240 - this.sub_5d2(var2)) / 2;
      int var4 = 160 - this.var_550 / 2;
      GameEngine.screenBuffer[0] = Integer.MIN_VALUE;
      sub_159(GameEngine.screenBuffer, 0, 38400);
      var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 0, 240, 160, true);
      var1.drawRGB(GameEngine.screenBuffer, 0, 240, 0, 160, 240, 160, true);
      this.drawLargeString(var2, var1, var3, var4);
      this.flushScreenBuffer();
   }

   private int drawDialogOverlay(Graphics var1, int var2) {
      try {
         boolean var3 = false;
         int var4 = this.var_550 + 6;
         Image var5 = Image.createImage(240, 320);
         Image var6 = Image.createImage("/gamedata/sprites/bkg_cut.png");
         Image var7 = Image.createImage("/gamedata/sprites/player.png");
         Image var8 = var2 != 0 && var2 != 9 ? Image.createImage(var2 == 8 ? "/gamedata/sprites/ag_hurt.png" : "/gamedata/sprites/ag.png") : null;
         Image var9 = var2 == 7 ? Image.createImage("/gamedata/sprites/doctor.png") : null;
         this.smallFontImage = Image.createImage("/gamedata/sprites/font_cut.png");
         int var10 = 240 - var7.getWidth() - 6;
         Graphics var11;
         (var11 = var5.getGraphics()).setColor(16711680);
         var11.drawImage(var6, 0, 0, 20);
         var11.drawImage(var7, 2, 2, 20);
         int var12 = 162;
         int var13 = 2 * (320 - var4) / 3 + 2;
         int var14 = (316 - var4) / this.var_6d3;
         int[][] var15 = new int[3][];
         int[] var16 = new int[]{0, 0, 0};
         String[] var17 = new String[3];
         if (var8 != null) {
            int[][] var10000;
            byte var10001;
            if (var9 != null) {
               var12 = (320 - var4) / 3 + 2;
               var11.drawImage(var8, 238 - var8.getWidth(), var12, 20);
               var11.drawImage(var9, 238 - var9.getWidth(), var13, 20);
               var14 = (316 - var4) / (this.var_6d3 * 3);
               var15[1] = new int[var14];
               var10000 = var15;
               var10001 = 2;
            } else {
               var11.drawImage(var8, 238 - var8.getWidth(), 162, 20);
               var14 = (316 - var4) / (this.var_6d3 * 2);
               var10000 = var15;
               var10001 = 1;
            }

            var10000[var10001] = new int[var14];
         }

         var15[0] = new int[var14];
         this.sub_1e3(var1, var5);
         this.drawLargeString("back", var1, 240 - this.sub_5d2("back") - 3, 320 - this.var_550 - 3);
         this.drawLargeString("pause", var1, 3, 320 - this.var_550 - 3);
         int[] var18 = new int[]{0, 0, 0};
         int[] var19 = new int[]{0, 0, 0};
         int[] var20 = new int[]{0, 0, 0};
         int[] var21 = new int[]{0, 0, 0};
         long[] var22 = new long[]{0L, 0L, 0L};
         boolean[] var23 = new boolean[]{false, false, false};

         for(int var24 = 0; var24 < storyText[var2].length; ++var24) {
            String var25;
            int var26;
            int var27;
            byte var28;
            label165: {
               var25 = storyText[var2][var24];
               var26 = var7.getWidth() + 4;
               var27 = 2;
               var28 = 0;
               byte var58;
               if (var25.startsWith("A")) {
                  var26 = 2;
                  var27 = var12;
                  var58 = 1;
               } else {
                  if (!var25.startsWith("M")) {
                     break label165;
                  }

                  var26 = 2;
                  var27 = var13;
                  var58 = 2;
               }

               var28 = var58;
            }

            int var29 = var26 + var10;
            int var30 = var27 + var14 * this.var_6d3;
            int var31 = var26;
            int var32 = var27;
            var18[var28] = 0;
            var17[var28] = var25;
            delay(500);
            if (var23[var28]) {
               var23[var28] = false;
               var1.drawRegion(var5, var20[var28], var19[var28], var10, var21[var28] - var19[var28], 0, var20[var28], var19[var28], 20);
            }

            var16[var28] = 0;
            var15[var28][var16[var28]] = 1;

            for(int var33 = 1; var33 < var25.length(); ++var33) {
               int var10002;
               char var34;
               int var37;
               int var38;
               int var39;
               if ((var34 = var25.charAt(var33)) == ' ') {
                  if (var31 + this.var_75f > var29) {
                     var31 = var26;
                     if (var16[var28] >= var14 - 1) {
                        this.sub_493(var1, var5, var25, var15[var28], var33 + 1, var26, var27, var30, var10);
                     } else {
                        var32 += this.var_6d3;
                        var18[var28] += this.var_6d3;
                        var10002 = var16[var28]++;
                        var15[var28][var16[var28]] = var33 + 1;
                     }
                  } else {
                     int var35;
                     if ((var35 = var25.indexOf(32, var33 + 1)) == -1) {
                        var35 = var25.length();
                     }

                     String var36 = var25.substring(var33, var35);
                     var37 = this.sub_5ef(var36);
                     if (var31 + this.var_75f + var37 > var29) {
                        var31 = var26;
                        if (var16[var28] >= var14 - 1) {
                           this.sub_493(var1, var5, var25, var15[var28], var33 + 1, var26, var27, var30, var10);
                        } else {
                           var32 += this.var_6d3;
                           var18[var28] += this.var_6d3;
                           var10002 = var16[var28]++;
                           var15[var28][var16[var28]] = var33 + 1;
                        }
                     } else {
                        var31 += this.var_75f;
                     }
                  }
               } else {
                  int[] var55;
                  int var56 = (var55 = this.getFontCoordinates(var34))[1] * this.smallFontCharsPerRow + var55[0];
                  var37 = this.var_6a8[var56];
                  var38 = this.var_691[var56];
                  var39 = var55[1] * this.var_6d3;
                  if (var31 + var37 + 1 > var29) {
                     var31 = var26;
                     if (var16[var28] >= var14 - 1) {
                        this.sub_493(var1, var5, var25, var15[var28], var33, var26, var27, var30, var10);
                     } else {
                        var32 += this.var_6d3;
                        var18[var28] += this.var_6d3;
                        var10002 = var16[var28]++;
                        var15[var28][var16[var28]] = var33;
                     }
                  }

                  var1.drawRegion(this.smallFontImage, var38, var39, var37, this.var_6d3, 0, var31, var32, 20);
                  var31 += var37 + 1;
               }

               this.flushScreenBuffer();
               delay(var34 == ',' ? 300 : (var34 != '.' && var34 != '?' && var34 != '!' ? 50 : 400));
               long var57 = System.currentTimeMillis();

               for(var37 = 0; var37 < 3; ++var37) {
                  if (var23[var37] && var57 > var22[var37]) {
                     var23[var37] = false;
                     var1.drawRegion(var5, var20[var37], var19[var37], var10, var21[var37] - var19[var37], 0, var20[var37], var19[var37], 20);
                     var16[var37] = 0;
                     var17[var37] = null;
                  }
               }

               if (GameEngine.inputRun) {
                  GameEngine.inputRun = false;
                  var1.drawRegion(var5, 3, 320 - this.var_550 - 3, this.sub_5d2("pause"), this.var_550, 0, 3, 320 - this.var_550 - 3, 20);
                  this.drawLargeString("resume", var1, 3, 320 - this.var_550 - 3);
                  this.flushScreenBuffer();

                  while(!GameEngine.inputRun && !GameEngine.inputBack && !this.isGamePaused && !GameEngine.inputFire) {
                     yieldToOtherThreads();
                  }
               }

               if (GameEngine.inputRun) {
                  var1.drawRegion(var5, 3, 320 - this.var_550 - 3, this.sub_5d2("resume"), this.var_550, 0, 3, 320 - this.var_550 - 3, 20);
                  this.drawLargeString("pause", var1, 3, 320 - this.var_550 - 3);
                  this.flushScreenBuffer();
                  GameEngine.inputRun = false;
               }

               if (GameEngine.inputBack || this.isGamePaused) {
                  GameEngine.inputRun = false;
                  GameEngine.inputBack = false;
                  if ((var37 = this.showMenuScreen(var1, false)) != 32) {
                     this.smallFontImage = null;
                     return var37;
                  }

                  var1.drawImage(var5, 0, 0, 20);
                  this.drawLargeString("back", var1, 240 - this.sub_5d2("back") - 3, 320 - this.var_550 - 3);
                  this.drawLargeString("pause", var1, 3, 320 - this.var_550 - 3);

                  for(var38 = 0; var38 < 3; ++var38) {
                     var39 = var38 == var28 ? var33 : (var17[var38] != null ? var17[var38].length() : 0);
                     if (var15[var38] != null) {
                        int var40;
                        int var41;
                        int var59;
                        label223: {
                           var40 = 0;
                           var41 = 0;
                           switch(var38) {
                           case 0:
                              var40 = var7.getWidth() + 4;
                              var59 = 2;
                              break;
                           case 1:
                              var40 = 2;
                              var59 = var12;
                              break;
                           case 2:
                              var40 = 2;
                              var59 = var13;
                              break;
                           default:
                              break label223;
                           }

                           var41 = var59;
                        }

                        for(int var42 = 0; var42 <= var16[var38]; ++var42) {
                           int var43 = var15[var38][var42];
                           int var44 = var42 + 1 <= var16[var38] ? var15[var38][var42 + 1] : (var17[var38] != null ? var17[var38].length() : 0);
                           if (var39 + 1 < var44) {
                              var44 = var39 + 1;
                           }

                           int var45 = var40;

                           for(int var46 = var43; var46 < var44; ++var46) {
                              char var47;
                              int var60;
                              if ((var47 = var17[var38].charAt(var46)) == ' ') {
                                 var59 = var45;
                                 var60 = this.var_75f;
                              } else {
                                 int[] var48;
                                 int var49 = (var48 = this.getFontCoordinates(var47))[1] * this.smallFontCharsPerRow + var48[0];
                                 int var50 = this.var_6a8[var49];
                                 int var51 = this.var_691[var49];
                                 int var52 = var48[1] * this.var_6d3;
                                 var1.drawRegion(this.smallFontImage, var51, var52, var50, this.var_6d3, 0, var45, var41, 20);
                                 var59 = var45;
                                 var60 = var50 + 1;
                              }

                              var45 = var59 + var60;
                           }

                           var41 += this.var_6d3;
                        }
                     }
                  }

                  this.flushScreenBuffer();
               }

               if (GameEngine.inputFire) {
                  GameEngine.inputFire = false;
                  this.smallFontImage = null;
                  return -1;
               }

               yieldToOtherThreads();
            }

            var18[var28] = 0;
            var23[var28] = true;
            var20[var28] = var26;
            var19[var28] = var27;
            var21[var28] = var30;
            var22[var28] = System.currentTimeMillis() + 5000L;
            delay(500);
         }

         delay(5000);
         this.smallFontImage = null;
      } catch (Exception var53) {
      } catch (OutOfMemoryError var54) {
      }

      return -1;
   }

   private void sub_493(Graphics var1, Image var2, String var3, int[] var4, int var5, int var6, int var7, int var8, int var9) {
      var1.drawRegion(var2, var6, var7, var9, var8 - var7, 0, var6, var7, 20);
      int var10 = var7;

      for(int var11 = 1; var11 < var4.length; ++var11) {
         int var12 = var4[var11];
         var4[var11 - 1] = var12;
         int var13 = var11 + 1 < var4.length ? var4[var11 + 1] : var5;
         int var14 = var6;

         for(int var15 = var12; var15 < var13; ++var15) {
            int var10000;
            char var16;
            int var10001;
            if ((var16 = var3.charAt(var15)) == ' ') {
               var10000 = var14;
               var10001 = this.var_75f;
            } else {
               int[] var17;
               int var18 = (var17 = this.getFontCoordinates(var16))[1] * this.smallFontCharsPerRow + var17[0];
               int var19 = this.var_6a8[var18];
               int var20 = this.var_691[var18];
               int var21 = var17[1] * this.var_6d3;
               var1.drawRegion(this.smallFontImage, var20, var21, var19, this.var_6d3, 0, var14, var10, 20);
               var10000 = var14;
               var10001 = var19 + 1;
            }

            var14 = var10000 + var10001;
         }

         var10 += this.var_6d3;
      }

      var4[var4.length - 1] = var5;
   }

   public static void delay(int var0) {
      long var1 = System.currentTimeMillis();

      do {
         yieldToOtherThreads();
      } while(System.currentTimeMillis() - var1 < (long)var0);

   }

   private void sub_547(int var1, Graphics var2, int var3, int var4) {
      String var5 = Integer.toString(var1);
      int var6 = this.sub_5d2(var5) / 2;
      this.drawLargeString(var5, var2, var3 - var6, var4);
   }

   public static void freeMemory() {
      System.gc();
   }

   private int sub_5d2(String var1) {
      var1 = var1.toLowerCase();
      int var2 = 0;

      for(int var3 = 0; var3 < var1.length(); ++var3) {
         int var10000;
         int var10001;
         char var4;
         if ((var4 = var1.charAt(var3)) == ' ') {
            var10000 = var2;
            var10001 = this.var_59b;
         } else {
            int[] var5 = this.sub_62f(var4);
            int var6 = this.var_65d[var5[1] * this.var_4db + var5[0]];
            var10000 = var2;
            var10001 = var6;
         }

         var2 = var10000 + var10001;
      }

      return var2;
   }

   private int sub_5ef(String var1) {
      int var2 = 0;

      for(int var3 = 0; var3 < var1.length(); ++var3) {
         int var10000;
         int var10001;
         char var4;
         if ((var4 = var1.charAt(var3)) == ' ') {
            var10000 = var2;
            var10001 = this.var_75f;
         } else {
            int[] var5 = this.getFontCoordinates(var4);
            int var6 = this.var_6a8[var5[1] * this.smallFontCharsPerRow + var5[0]];
            var10000 = var2;
            var10001 = var6 + 1;
         }

         var2 = var10000 + var10001;
      }

      return var2;
   }

   private int[] sub_62f(char var1) {
      int[] var2 = new int[]{this.var_4db - 1, 2};
      int[] var10000;
      byte var10001;
      byte var10002;
      if (var1 >= 'a' && var1 <= 'r') {
         var2[0] = var1 - 97;
         var10000 = var2;
         var10001 = 1;
         var10002 = 0;
      } else if (var1 >= 's' && var1 <= 'z') {
         var2[0] = var1 - 115;
         var10000 = var2;
         var10001 = 1;
         var10002 = 1;
      } else if (var1 >= '0' && var1 <= '9') {
         var2[0] = var1 - 48;
         var10000 = var2;
         var10001 = 1;
         var10002 = 2;
      } else {
         var2[1] = 1;
         switch(var1) {
         case '!':
            var10000 = var2;
            var10001 = 0;
            var10002 = 10;
            break;
         case '\'':
            var10000 = var2;
            var10001 = 0;
            var10002 = 15;
            break;
         case ',':
            var10000 = var2;
            var10001 = 0;
            var10002 = 9;
            break;
         case '.':
            var10000 = var2;
            var10001 = 0;
            var10002 = 8;
            break;
         case '/':
            var10000 = var2;
            var10001 = 0;
            var10002 = 14;
            break;
         case ':':
            var10000 = var2;
            var10001 = 0;
            var10002 = 12;
            break;
         case ';':
            var10000 = var2;
            var10001 = 0;
            var10002 = 13;
            break;
         case '?':
            var10000 = var2;
            var10001 = 0;
            var10002 = 11;
            break;
         default:
            return var2;
         }
      }

      var10000[var10001] = var10002;
      return var2;
   }

   private int[] getFontCoordinates(char character) {
      int[] coords = new int[]{this.smallFontCharsPerRow - 1, 2};
      int[] var10000;
      byte var10001;
      byte var10002;
      if (character >= 'A' && character <= 'Z') {
         coords[0] = character - 65;
         var10000 = coords;
         var10001 = 1;
         var10002 = 0;
      } else if (character >= 'a' && character <= 'z') {
         coords[0] = character - 97;
         var10000 = coords;
         var10001 = 1;
         var10002 = 1;
      } else if (character >= '0' && character <= '9') {
         coords[0] = character - 48;
         var10000 = coords;
         var10001 = 1;
         var10002 = 2;
      } else {
         coords[1] = 2;
         switch(character) {
         case '!':
            var10000 = coords;
            var10001 = 0;
            var10002 = 12;
            break;
         case '"':
         case '#':
         case '$':
         case '%':
         case '&':
         case '(':
         case ')':
         case '*':
         case '+':
         case '0':
         case '1':
         case '2':
         case '3':
         case '4':
         case '5':
         case '6':
         case '7':
         case '8':
         case '9':
         case '<':
         case '=':
         case '>':
         default:
            return coords;
         case '\'':
            var10000 = coords;
            var10001 = 0;
            var10002 = 18;
            break;
         case ',':
            var10000 = coords;
            var10001 = 0;
            var10002 = 11;
            break;
         case '-':
            var10000 = coords;
            var10001 = 0;
            var10002 = 17;
            break;
         case '.':
            var10000 = coords;
            var10001 = 0;
            var10002 = 10;
            break;
         case '/':
            var10000 = coords;
            var10001 = 0;
            var10002 = 16;
            break;
         case ':':
            var10000 = coords;
            var10001 = 0;
            var10002 = 14;
            break;
         case ';':
            var10000 = coords;
            var10001 = 0;
            var10002 = 15;
            break;
         case '?':
            var10000 = coords;
            var10001 = 0;
            var10002 = 13;
            break;
         case '@':
            var10000 = coords;
            var10001 = 0;
            var10002 = 19;
         }
      }

      var10000[var10001] = var10002;
      return coords;
   }

   private void drawLargeString(String var1, Graphics var2, int var3, int var4) {
      var1 = var1.toLowerCase();

      for(int var5 = 0; var5 < var1.length(); ++var5) {
         char var6;
         int var10000;
         int var10001;
         if ((var6 = var1.charAt(var5)) == ' ') {
            var10000 = var3;
            var10001 = this.var_59b;
         } else {
            int[] var7;
            int var8 = (var7 = this.sub_62f(var6))[1] * this.var_4db + var7[0];
            int var9 = this.var_65d[var8];
            int var10 = this.var_5fa[var8];
            int var11 = var7[1] * this.var_550;
            var2.drawRegion(this.largeFontImage, var10, var11, var9, this.var_550, 0, var3, var4, 20);
            var10000 = var3;
            var10001 = var9;
         }

         var3 = var10000 + var10001;
      }

   }

   private void drawSmallString(String var1, Graphics var2, int var3, int var4) {
      for(int var5 = 0; var5 < var1.length(); ++var5) {
         char var6;
         int var10000;
         int var10001;
         if ((var6 = var1.charAt(var5)) == ' ') {
            var10000 = var3;
            var10001 = this.var_75f;
         } else {
            int[] var7;
            int var8 = (var7 = this.getFontCoordinates(var6))[1] * this.smallFontCharsPerRow + var7[0];
            int var9 = this.var_6a8[var8];
            int var10 = this.var_691[var8];
            int var11 = var7[1] * this.var_6d3;
            var2.drawRegion(this.smallFontImage, var10, var11, var9, this.var_6d3, 0, var3, var4, 20);
            var10000 = var3;
            var10001 = var9 + 1;
         }

         var3 = var10000 + var10001;
      }

   }

   public static void loadSaveData() {
      try {
         String var0;
         label33: {
            var0 = "data";
            StringBuffer var10000;
            String var10001;
            if (GameEngine.difficultyLevel == 0) {
               var10000 = (new StringBuffer()).append(var0);
               var10001 = "e";
            } else {
               if (GameEngine.difficultyLevel != 2) {
                  break label33;
               }

               var10000 = (new StringBuffer()).append(var0);
               var10001 = "h";
            }

            var0 = var10000.append(var10001).toString();
         }

         RecordStore var1 = RecordStore.openRecordStore(var0, true);
         saveData = new byte[9][];
         int var2;
         if ((var2 = var1.getNumRecords()) > 9) {
            var2 = 9;
         }

         for(int var3 = 0; var3 < var2; ++var3) {
            saveData[var3] = var1.getRecord(var3 + 1);
         }

         var1.closeRecordStore();
      } catch (RecordStoreException var4) {
      } catch (OutOfMemoryError var5) {
      }
   }

   public static void loadGameState(int var0) {
      GameEngine.playerHealth = saveData[var0][0];
      GameEngine.playerArmor = saveData[var0][1];

      for(int var1 = 0; var1 < 9; ++var1) {
         GameEngine.weaponsAvailable[var1] = saveData[var0][2 + var1] == 1;
         GameEngine.ammoCounts[var1] = (saveData[var0][11 + var1] & 255) + ((saveData[var0][20 + var1] & 255) << 8);
      }

      GameEngine.currentWeapon = saveData[var0][29];
      GameEngine.pendingWeaponSwitch = GameEngine.currentWeapon;
   }

   public static void saveGameState(int var0) {
      saveData[var0] = new byte[30];
      saveData[var0][0] = (byte) GameEngine.playerHealth;
      saveData[var0][1] = (byte) GameEngine.playerArmor;

      for(int var1 = 0; var1 < 9; ++var1) {
         saveData[var0][2 + var1] = (byte)(GameEngine.weaponsAvailable[var1] ? 1 : 0);
         saveData[var0][11 + var1] = (byte)(GameEngine.ammoCounts[var1] & 255);
         saveData[var0][20 + var1] = (byte)(GameEngine.ammoCounts[var1] >> 8 & 255);
      }

      saveData[var0][29] = (byte) GameEngine.currentWeapon;
      writeSaveData();
   }

   public static void writeSaveData() {
      try {
         String var0;
         label39: {
            var0 = "data";
            StringBuffer var10000;
            String var10001;
            if (GameEngine.difficultyLevel == 0) {
               var10000 = (new StringBuffer()).append(var0);
               var10001 = "e";
            } else {
               if (GameEngine.difficultyLevel != 2) {
                  break label39;
               }

               var10000 = (new StringBuffer()).append(var0);
               var10001 = "h";
            }

            var0 = var10000.append(var10001).toString();
         }

         RecordStore var1;
         int var2 = (var1 = RecordStore.openRecordStore(var0, true)).getNumRecords();

         for(int var3 = 0; var3 < 9; ++var3) {
            int var4 = saveData[var3] == null ? 0 : saveData[var3].length;
            if (var2 > var3) {
               var1.setRecord(var3 + 1, saveData[var3], 0, var4);
            } else if (var4 > 0) {
               var1.addRecord(saveData[var3], 0, var4);
            }
         }

         var1.closeRecordStore();
      } catch (RecordStoreException var5) {
      } catch (OutOfMemoryError var6) {
      }
   }

   public static void loadSettingsFromRMS() {
      try {
         RecordStore var0 = RecordStore.openRecordStore("settings", true);
         Object var1 = null;
         if (var0.getNumRecords() > 0) {
            byte[] var5;
            soundEnabled = (var5 = var0.getRecord(1))[0];
            musicEnabled = var5[1];
            vibrationEnabled = var5[2];
            gameProgressFlags = var5[3];
         }

         var0.closeRecordStore();
      } catch (RecordStoreException var3) {
      } catch (OutOfMemoryError var4) {
      }
   }

   public static void saveSettingsToRMS() {
      try {
         RecordStore var0;
         int var1 = (var0 = RecordStore.openRecordStore("settings", true)).getNumRecords();
         byte[] var2;
         (var2 = new byte[16])[0] = soundEnabled;
         var2[1] = musicEnabled;
         var2[2] = vibrationEnabled;
         var2[3] = gameProgressFlags;
         if (var1 > 0) {
            var0.setRecord(1, var2, 0, 16);
         } else {
            var0.addRecord(var2, 0, 16);
         }

         var0.closeRecordStore();
      } catch (RecordStoreException var3) {
      } catch (OutOfMemoryError var4) {
      }
   }

   public static void playSound(int var0, boolean var1, int var2, int var3) {
      boolean var4;
      if (!(var4 = var0 > 0) || soundEnabled != 0) {
         int var5 = var1 ? -1 : 1;
         audioManager.setVolume(var2);
         audioManager.playSound(var0, var5, var3);
      }
   }

   public static void stopCurrentSound() {
      audioManager.stopCurrentSound();
   }

   public static void vibrateDevice(int var0) {
      if (vibrationEnabled != 0) {
         try {
            CovertOps3D.display.vibrate(var0);
         } catch (Exception var1) {
         } catch (OutOfMemoryError var2) {
         }
      }
   }

   private void flushScreenBuffer() {
      this.flushGraphics();
   }

   private static void yieldToOtherThreads() {
      Thread.yield();
   }
}
