import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Hashtable;

public class LevelLoader {
    public static int levelVariant = 0;
    public static GameWorld gameWorld = null;
    private static int resourceLoadState;
    private static Sprite[] spriteTable;
    static Texture[] textureTable;
    private static Hashtable paletteCache;
    static Texture defaultErrorTexture;

    private static Sprite getSprite(byte var0) {
       if (var0 == 51) {
          return null;
       } else {
          Sprite var1;
          return (var1 = spriteTable[var0]) != null && var1.pixelData != null ? var1 : null;
       }
    }

    static Texture getTexture(byte var0) {
       if (var0 == 0) {
          return defaultErrorTexture;
       } else {
          Texture var1;
          return (var1 = textureTable[var0 + 128]) != null && var1.width > 0 ? var1 : defaultErrorTexture;
       }
    }

    static void initResourceArrays() {
       spriteTable = new Sprite[128];
       textureTable = new Texture[256];
       paletteCache = new Hashtable();
    }

    private static void unloadAllResources() {
       gameWorld = null;
       resourceLoadState = 0;

       int var0;
       for(var0 = 0; var0 < 128; ++var0) {
          spriteTable[var0] = null;
       }

       for(var0 = 0; var0 < 256; ++var0) {
          textureTable[var0] = null;
       }

       paletteCache.clear();
    }

    public static boolean loadMapData(String var0, boolean var1) {
       unloadAllResources();

       try {
          InputStream var3 = (new Object()).getClass().getResourceAsStream(var0);
          DataInputStream var4 = new DataInputStream(var3);
          gameWorld = new GameWorld();
          var4.readByte();
          Point2D[] var6 = new Point2D[BinaryUtils.readIntLE(var4) / 4];

          for(int var7 = 0; var7 < var6.length; ++var7) {
             var6[var7] = new Point2D(BinaryUtils.readShortLE(var4) << 16, BinaryUtils.readShortLE(var4) << 16);
          }

          gameWorld.setVertices(var6);
          WallDefinition[] var25 = new WallDefinition[BinaryUtils.readIntLE(var4) / 11];

          int var8;
          short var14;
          short var15;
          for(var8 = 0; var8 < var25.length; ++var8) {
             short var9 = BinaryUtils.readShortLE(var4);
             short var10 = BinaryUtils.readShortLE(var4);
             byte var11 = var4.readByte();
             byte var12 = var4.readByte();
             byte var13 = var4.readByte();
             var14 = BinaryUtils.readShortLE(var4);
             var15 = BinaryUtils.readShortLE(var4);
             var25[var8] = new WallDefinition(var9, var10, var14, var15, var11, var12, var13);
          }

          gameWorld.wallDefinitions = var25;
          int var5 = BinaryUtils.readIntLE(var4);
          if (levelVariant == 0) {
             levelVariant = 1;
          }

          var8 = var5 / 10;
          GameObject[] var26 = null;
          if (var1) {
             var26 = new GameObject[var8];
          }

          short var31;
          short var33;
          for(int var27 = 0; var27 < var8; ++var27) {
             short var29 = BinaryUtils.readShortLE(var4);
             var31 = BinaryUtils.readShortLE(var4);
             var33 = BinaryUtils.readShortLE(var4);
             var14 = BinaryUtils.readShortLE(var4);
             var15 = BinaryUtils.readShortLE(var4);
             GameObject[] var10000;
             int var10001;
             GameObject var10002;
             if (var14 >= 1 && var14 <= 4) {
                if (levelVariant == var14) {
                   gameWorld.worldOrigin = new Transform3D(var29 << 16, 0, var31 << 16, -var33 * 1144 + 102943);
                }

                if (!var1) {
                   continue;
                }

                var10000 = var26;
                var10001 = var27;
                var10002 = null;
             } else {
                if (!var1) {
                   continue;
                }

                var10000 = var26;
                var10001 = var27;
                var10002 = new GameObject(new Transform3D(var29 << 16, 0, var31 << 16, -var33 * 1144 + 102943), var33, var14, var15);
             }

             var10000[var10001] = var10002;
          }

          if (var1) {
             gameWorld.staticObjects = var26;
          }

          WallSurface[] var28 = new WallSurface[BinaryUtils.readIntLE(var4) / 8];

          byte var16;
          byte var38;
          for(int var30 = 0; var30 < var28.length; ++var30) {
             var31 = BinaryUtils.readShortLE(var4);
             var33 = BinaryUtils.readShortLE(var4);
             byte var36 = remapLegacyTextureId(var4.readByte());
             var38 = remapLegacyTextureId(var4.readByte());
             var16 = remapLegacyTextureId(var4.readByte());
             byte var17 = var4.readByte();
             var28[var30] = new WallSurface(var36, var16, var38, var17, var31, var33);
             if (var36 != 0) {
                preloadTexture(var36);
             }

             if (var38 != 0) {
                preloadTexture(var38);
             }

             if (var16 != 0) {
                preloadTexture(var16);
             }
          }

          gameWorld.wallSurfaces = var28;
          SectorData[] var32 = new SectorData[BinaryUtils.readIntLE(var4) / 12];
          short var42 = 0;

          while(true) {
             var31 = var42;
             short var18;
             short var44;
             if (var42 >= var32.length) {
                gameWorld.sectors = var32;
                BSPNode[] var34 = new BSPNode[BinaryUtils.readIntLE(var4) / 12];

                short var41;
                int var48;
                for(int var35 = 0; var35 < var34.length; ++var35) {
                   var14 = BinaryUtils.readShortLE(var4);
                   var15 = BinaryUtils.readShortLE(var4);
                   var41 = BinaryUtils.readShortLE(var4);
                   var44 = BinaryUtils.readShortLE(var4);
                   int var46 = BinaryUtils.readShortLE(var4) & '\uffff';
                   var48 = BinaryUtils.readShortLE(var4) & '\uffff';
                   var34[var35] = new BSPNode(var14 << 16, var15 << 16, var41 << 16, var44 << 16, var46, var48);
                }

                gameWorld.bspNodes = var34;
                Sector[] var37 = new Sector[(var5 = BinaryUtils.readIntLE(var4)) / 4];

                for(int var39 = 0; var39 < var37.length; ++var39) {
                   var15 = BinaryUtils.readShortLE(var4);
                   var41 = BinaryUtils.readShortLE(var4);
                   var37[var39] = new Sector(var15, var41);
                }

                gameWorld.bspSectors = var37;
                BSPNode.visibleSectorsList = new Sector[var5 / 4];
                WallSegment[] var40 = new WallSegment[BinaryUtils.readIntLE(var4) / 9];

                int var43;
                for(var43 = 0; var43 < var40.length; ++var43) {
                   var41 = BinaryUtils.readShortLE(var4);
                   var44 = BinaryUtils.readShortLE(var4);
                   var18 = BinaryUtils.readShortLE(var4);
                   boolean var49 = var4.readByte() == 0;
                   short var20 = BinaryUtils.readShortLE(var4);
                   var40[var43] = new WallSegment(var41, var44, var18, var49, var20);
                }

                gameWorld.wallSegments = var40;
                BinaryUtils.readIntLE(var4);
                boolean[][] var45 = new boolean[var43 = gameWorld.sectors.length][var43];
                int var47 = 0;
                var48 = 0;

                while(var48 < var43) {
                   int var50 = var4.readByte() & 255;

                   for(int var21 = 0; var21 < 8 & var48 < var43; ++var21) {
                      var45[var47][var48] = (var50 & 1 << var21) == 1 << var21;
                      ++var47;
                      if (var47 >= var43) {
                         var47 = 0;
                         ++var48;
                      }
                   }
                }

                for(var48 = 0; var48 < var43; ++var48) {
                   gameWorld.sectors[var48].visitedFlags = var45[var48];
                }

                var4.close();
                break;
             }

             var33 = BinaryUtils.readShortLE(var4);
             var14 = BinaryUtils.readShortLE(var4);
             var38 = remapLegacyTextureId(var4.readByte());
             var16 = remapLegacyTextureId(var4.readByte());
             var44 = BinaryUtils.readShortLE(var4);
             var18 = BinaryUtils.readShortLE(var4);
             short var19 = BinaryUtils.readShortLE(var4);
             var32[var31] = new SectorData(var31, var33, var14, var38, var16, (short)(var44 >> 4 & 15), var18, var19);
             if (var38 != 0) {
                preloadSprite(var38);
             }

             if (var16 != 0) {
                preloadSprite(var16);
             }

             var42 = (short)(var31 + 1);
          }
       } catch (Exception var22) {
          return false;
       } catch (OutOfMemoryError var23) {
          return false;
       }

       int var24;
       if ((var24 = var0.lastIndexOf(47)) != -1) {
          var0.substring(0, var24 + 1);
       }

       resourceLoadState = 1;
       return true;
    }

    private static byte remapLegacyTextureId(byte var0) {
       byte var10000;
       switch(var0) {
       case 6:
          return 5;
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
       case 24:
       case 25:
       case 26:
       case 27:
       case 28:
       case 29:
       case 30:
       case 31:
       case 32:
       case 33:
       case 34:
       case 35:
       case 36:
       case 37:
       case 38:
       case 41:
       case 44:
       case 45:
       case 46:
       case 49:
       default:
          return var0;
       case 18:
       case 19:
       case 20:
       case 21:
       case 22:
       case 23:
          return 17;
       case 39:
       case 40:
          var10000 = 35;
          break;
       case 42:
       case 43:
          return 41;
       case 47:
       case 48:
          var10000 = 46;
          break;
       case 50:
          var10000 = 49;
       }

       preloadTexture(var10000);
       return var0;
    }

    public static boolean loadGameAssets(String var0, int var1, String var2, int var3) {
       if (resourceLoadState < 1) {
          throw new IllegalStateException();
       } else if (resourceLoadState > 1) {
          throw new IllegalStateException();
       } else {
          try {
             int var4 = 0;
             Hashtable var5 = new Hashtable();
             int var6 = 1;

             label233:
             while(true) {
                InputStream var7;
                DataInputStream var8;
                short var9;
                short var10;
                short var12;
                short var13;
                byte var15;
                int var16;
                short var17;
                short var18;
                int var19;
                int var40;
                int[] var41;
                int[][] var42;
                if (var6 > var1) {
                   for(var6 = 1; var6 <= var3; ++var6) {
                      if ((var7 = (new Object()).getClass().getResourceAsStream(var2 + Integer.toString(var6))) == null) {
                         throw new IllegalStateException();
                      }

                      if ((BinaryUtils.readShortBE(var8 = new DataInputStream(var7)) & '\uffff') != 39251) {
                         throw new IllegalStateException();
                      }

                      var9 = BinaryUtils.readShortBE(var8);
                      var10 = BinaryUtils.readShortBE(var8);
                      BinaryUtils.readIntBE(var8);

                      for(var16 = 0; var16 < var9; ++var16) {
                         var15 = var8.readByte();
                         var12 = BinaryUtils.readShortBE(var8);
                         var13 = BinaryUtils.readShortBE(var8);
                         var17 = BinaryUtils.readShortBE(var8);
                         var18 = BinaryUtils.readShortBE(var8);
                         short var43 = BinaryUtils.readShortBE(var8);
                         short var45 = BinaryUtils.readShortBE(var8);
                         int var44;
                         int var47 = (var44 = var12 * var13 * var45) / 8;
                         if (var44 % 8 > 0) {
                            ++var47;
                         }

                         if (isTextureRegistered(var15)) {
                            Texture var48 = new Texture(var15, var12, var13, var17, var18);
                            byte[] var49 = new byte[var47];
                            var8.readFully(var49, 0, var47);
                            byte[] var26 = null;

                            for(int var27 = 0; var27 < var12; ++var27) {
                               if ((var27 & 1) == 0) {
                                  var26 = new byte[var13];
                               }

                               decompressTexture(var49, var27 * var13, var26, 0, var13, var45, var27 & 1);
                               var48.setPixelData(var27, var26);
                            }

                            textureTable[var15 + 128] = var48;
                            var5.put(new Byte(var15), new Integer(var4 + var43));
                         } else {
                            BinaryUtils.skipBytes(var8, var47);
                         }
                      }

                      for(var16 = 0; var16 < var10; ++var16) {
                         var40 = BinaryUtils.readIntBE(var8);
                         if (!var5.contains(new Integer(var4 + var16))) {
                            BinaryUtils.skipBytes(var8, 4 * var40);
                         } else {
                            var41 = new int[var40];

                            for(var19 = 0; var19 < var40; ++var19) {
                               var41[var19] = BinaryUtils.readIntBE(var8);
                            }

                            var42 = Texture.createColorPalettes(var41);
                            paletteCache.put(Integer.toString(var4 + var16), var42);
                         }
                      }

                      var4 += var10;
                      var8.close();
                   }

                   for(var6 = 0; var6 < 128; ++var6) {
                      Sprite var30;
                      if ((var30 = spriteTable[var6]) != null) {
                         if (var30.pixelData == null) {
                            throw new IllegalStateException();
                         }

                         var30.colorPalettes = (int[][])((int[][]) paletteCache.get(((Integer)((Integer)var5.get(new Byte(var30.spriteId)))).toString()));
                      }
                   }

                   byte[][] var31 = (byte[][])null;
                   byte[][] var32 = (byte[][])null;
                   byte[][] var33 = (byte[][])null;
                   Texture var34;
                   if ((var34 = textureTable[163]) != null) {
                      var31 = var34.pixelData;
                   }

                   if ((var34 = textureTable[174]) != null) {
                      var32 = var34.pixelData;
                   }

                   if ((var34 = textureTable[177]) != null) {
                      var33 = var34.pixelData;
                   }

                   for(int var35 = 0; var35 < 256; ++var35) {
                      Texture var11;
                      if ((var11 = textureTable[var35]) != null) {
                         byte var38;
                         label150: {
                            byte var10000;
                            switch(var38 = var11.textureType) {
                            case 35:
                               var11.compositeTexture(var31, 0, 0, 64, 128, 0, 0, true);
                            case 36:
                            case 37:
                            case 38:
                            case 41:
                            case 42:
                            case 43:
                            case 44:
                            case 45:
                            default:
                               break label150;
                            case 39:
                               var11.compositeTexture(var31, 0, 0, 64, 128, 0, 0, true);
                               var11.compositeTexture(var31, 0, 128, 64, 18, 0, 0, false);
                               var11.compositeTexture(var31, 64, 17, 10, 111, 54, 17, false);
                               var10000 = 35;
                               break;
                            case 40:
                               var11.compositeTexture(var31, 0, 0, 64, 128, 0, 0, true);
                               var11.compositeTexture(var31, 0, 128, 64, 18, 0, 0, false);
                               var11.compositeTexture(var31, 74, 17, 10, 111, 0, 17, false);
                               var10000 = 35;
                               break;
                            case 46:
                               var11.compositeTexture(var32, 0, 0, 64, 128, 0, 0, true);
                               break label150;
                            case 47:
                               var11.compositeTexture(var32, 0, 0, 64, 128, 0, 0, true);
                               var11.compositeTexture(var32, 64, 0, 14, 128, 50, 0, false);
                               var10000 = 46;
                               break;
                            case 48:
                               var11.compositeTexture(var32, 0, 0, 64, 128, 0, 0, true);
                               var11.compositeTexture(var32, 64, 0, 14, 128, 50, 0, false);
                               var11.compositeTexture(var32, 78, 0, 20, 24, 19, 43, false);
                               var10000 = 46;
                               break;
                            case 49:
                               var11.compositeTexture(var33, 0, 0, 64, 128, 0, 0, true);
                               break label150;
                            case 50:
                               var11.compositeTexture(var33, 0, 0, 64, 128, 0, 0, true);
                               var11.compositeTexture(var33, 64, 0, 20, 27, 25, 19, false);
                               var10000 = 49;
                            }

                            var38 = var10000;
                         }

                         if (var11.width != 0) {
                            var11.colorPalettes = (int[][])((int[][]) paletteCache.get(((Integer)((Integer)var5.get(new Byte(var38)))).toString()));
                         }
                      }
                   }

                   SectorData[] var36 = gameWorld.sectors;
                   int var37 = 0;

                   while(true) {
                      if (var37 >= var36.length) {
                         break label233;
                      }

                      SectorData var39 = null;
                      (var39 = var36[var37]).floorTexture = getSprite(var39.floorTextureId);
                      var39.ceilingTexture = getSprite(var39.ceilingTextureId);
                      ++var37;
                   }
                }

                if ((var7 = (new Object()).getClass().getResourceAsStream(var0 + Integer.toString(var6))) == null) {
                   throw new IllegalStateException();
                }

                if ((BinaryUtils.readShortBE(var8 = new DataInputStream(var7)) & '\uffff') != 39252) {
                   throw new IllegalStateException();
                }

                var9 = BinaryUtils.readShortBE(var8);
                var10 = BinaryUtils.readShortBE(var8);
                BinaryUtils.readIntBE(var8);

                for(var16 = 0; var16 < var9; ++var16) {
                   var15 = var8.readByte();
                   var12 = BinaryUtils.readShortBE(var8);
                   var13 = BinaryUtils.readShortBE(var8);
                   var17 = BinaryUtils.readShortBE(var8);
                   var18 = BinaryUtils.readShortBE(var8);
                   int var20 = (var19 = var12 * var13 * var18) / 8;
                   if (var19 % 8 > 0) {
                      ++var20;
                   }

                   if (isSpriteRegistered(var15)) {
                      if (var12 != 64 || var12 != 64) {
                         throw new IllegalStateException();
                      }

                      byte[] var21 = new byte[var20];
                      byte[] var22 = new byte[var12 * var13];
                      var8.readFully(var21, 0, var20);
                      decompressSprite(var21, 0, var22, 0, var12 * var13, var18);
                      spriteTable[var15] = new Sprite(var15, var22);
                      var5.put(new Byte(var15), new Integer(var4 + var17));
                   } else if (isTextureRegistered(var15)) {
                      Texture var46 = new Texture(var15, var12, var13, 0, 0);
                      byte[] var23 = new byte[var20];
                      var8.readFully(var23, 0, var20);
                      byte[] var24 = null;

                      for(int var25 = 0; var25 < var12; ++var25) {
                         if ((var25 & 1) == 0) {
                            var24 = new byte[var13];
                         }

                         decompressTexture(var23, var25 * var13, var24, 0, var13, var18, var25 & 1);
                         var46.setPixelData(var25, var24);
                      }

                      textureTable[var15 + 128] = var46;
                      var5.put(new Byte(var15), new Integer(var4 + var17));
                   } else {
                      BinaryUtils.skipBytes(var8, var20);
                   }
                }

                for(var16 = 0; var16 < var10; ++var16) {
                   var40 = BinaryUtils.readIntBE(var8);
                   if (!var5.contains(new Integer(var4 + var16))) {
                      BinaryUtils.skipBytes(var8, 4 * var40);
                   } else {
                      var41 = new int[var40];

                      for(var19 = 0; var19 < var40; ++var19) {
                         var41[var19] = BinaryUtils.readIntBE(var8);
                      }

                      var42 = Texture.createColorPalettes(var41);
                      paletteCache.put(Integer.toString(var4 + var16), var42);
                   }
                }

                var4 += var10;
                var8.close();
                ++var6;
             }
          } catch (Exception var28) {
             return false;
          } catch (OutOfMemoryError var29) {
             return false;
          }

          resourceLoadState = 2;
          return true;
       }
    }

    public static void preloadTexture(byte var0) {
       if (!isTextureRegistered(var0)) {
          textureTable[var0 + 128] = new Texture(var0, 0, 0, 0, 0);
       }

    }

    private static void preloadSprite(byte var0) {
       if (var0 != 51) {
          if (!isSpriteRegistered(var0)) {
             spriteTable[var0] = new Sprite(var0);
          }

       }
    }

    private static boolean isSpriteRegistered(int var0) {
       if (var0 >= 0 && var0 < 128) {
          return spriteTable[var0] != null;
       } else {
          return false;
       }
    }

    private static boolean isTextureRegistered(int var0) {
       var0 += 128;
       if (var0 >= 0 && var0 < 256) {
          return textureTable[var0] != null;
       } else {
          return false;
       }
    }

    /**
     * Decompresses a stream of packed pixel indices (palette indices) from a source
     * byte array into an unpacked destination array. This method implements a
     * BitReader logic that handles pixel indices spanning across byte boundaries.
     * The bits are read from Most Significant Bit (MSB) to Least Significant Bit (LSB).
     *
     * @param packedData The source byte array containing the packed pixel indices.
     * @param startPixelIndex The index of the first pixel in the logical stream to start decompression from.
     * This value is used to calculate the initial byte and bit offsets in {@code packedData}.
     * @param outputIndices The destination byte array to store the unpacked 8-bit pixel indices.
     * @param outputOffset The starting index in the destination array to write the first decompressed pixel.
     * @param pixelCount The number of pixels (indices) to decompress.
     * @param bitsPerPixel The bit depth of the packed indices (e.g., 4 for 16-color, 8 for 256-color).
     */
    public static void decompressSprite(
            byte[] packedData,
            int startPixelIndex,
            byte[] outputIndices,
            int outputOffset,
            int pixelCount,
            int bitsPerPixel)
    {
        // Calculate the initial position in the packed data stream
        int totalStartBits = startPixelIndex * bitsPerPixel;
        int currentByteIndex = totalStartBits / 8; // Current byte offset in packedData
        int currentBitOffset = totalStartBits % 8; // Current bit offset (0-7)

        // Mask to isolate the 'bitsPerPixel' value (e.g., for 4bpp, mask is 0xF)
        final int pixelValueMask = (1 << bitsPerPixel) - 1;
        final int outputEndIndex = outputOffset + pixelCount;

        for (int i = outputOffset; i < outputEndIndex; ++i) {
            // --- Bit Extraction Logic (Handles byte boundaries) ---

            byte currentByte = packedData[currentByteIndex];
            int extractedValue;

            // Calculate the number of bits to right-shift the current byte to align
            // the desired 'bitsPerPixel' block to the LSB position.
            // shiftRight: 8 - (bitsPerPixel + currentBitOffset)
            int shiftRight = 8 - (bitsPerPixel + currentBitOffset);

            if (shiftRight >= 0) {
                // Case 1: All bits are within the current byte
                // Shift right to align the value to the LSB.
                extractedValue = currentByte >> shiftRight;
            } else {
                // Case 2: Bits span across the current byte and the next byte
                int bitsFromNextByte = -shiftRight;

                // 1. Take the required bits from the current byte, shift them left.
                extractedValue = currentByte << bitsFromNextByte;

                // 2. Take the high bits from the next byte and combine (OR) them.
                //    Using `& 0xFF` ensures the next byte is treated as an unsigned value before shifting.
                extractedValue |= (packedData[currentByteIndex + 1] & 0xFF) >> (8 - bitsFromNextByte);
            }

            // --- Stream Position Update ---

            // Move the bit offset forward
            if ((currentBitOffset += bitsPerPixel) > 7) {
                currentBitOffset -= 8; // New bit offset (0-7)
                currentByteIndex++;    // Move to the next byte
            }

            // --- Write to Output ---

            // Apply the mask to isolate the pure pixel index and write it to the destination array.
            outputIndices[i] = (byte)(extractedValue & pixelValueMask);
        }
    }

    /**
     * Decompresses a stream of packed pixel indices, typically for scenarios where a lower bit-depth source
     * (e.g., 4bpp) is written into an 8bpp destination array in two separate passes (high and low nibble).
     *
     * The core bit-reading logic is the same as {@code decompressSprite}.
     *
     * @param packedData The source byte array containing the packed pixel indices.
     * @param startPixelIndex The index of the first pixel in the logical stream to start decompression from.
     * @param outputIndices The destination byte array (e.g., 8bpp) to store the combined pixel indices.
     * @param outputOffset The starting index in the destination array to write.
     * @param pixelCount The number of pixels (indices) to decompress.
     * @param bitsPerPixel The bit depth of the packed indices (must be compatible with the packing scheme, typically 4).
     * @param texturePartMode A flag indicating which part of the destination byte to write to (partially packed destination):
     * <ul>
     * <li><b>0:</b> Writes the extracted index to the <b>high nibble</b> (bits 7-4).</li>
     * <li><b>1:</b> Writes the extracted index to the <b>low nibble</b> (bits 3-0),
     * combining it with the high nibble already present in the byte.</li>
     * </ul>
     */
    private static void decompressTexture(
            byte[] packedData,
            int startPixelIndex,
            byte[] outputIndices,
            int outputOffset,
            int pixelCount,
            int bitsPerPixel,
            int texturePartMode)
    {
        // Decompression setup (identical to decompressSprite)
        int totalStartBits = startPixelIndex * bitsPerPixel;
        int currentByteIndex = totalStartBits / 8;
        int currentBitOffset = totalStartBits % 8;
        final int pixelValueMask = (1 << bitsPerPixel) - 1;
        final int outputEndIndex = outputOffset + pixelCount;

        for (int i = outputOffset; i < outputEndIndex; ++i) {
            // --- Bit Extraction Logic (Identical to decompressSprite) ---

            byte currentByte = packedData[currentByteIndex];
            int extractedValue;
            int shiftRight = 8 - (bitsPerPixel + currentBitOffset);

            if (shiftRight >= 0) {
                extractedValue = currentByte >> shiftRight;
            } else {
                int bitsFromNextByte = -shiftRight;
                extractedValue = currentByte << bitsFromNextByte;
                extractedValue |= (packedData[currentByteIndex + 1] & 0xFF) >> (8 - bitsFromNextByte);
            }

            // --- Stream Position Update ---

            if ((currentBitOffset += bitsPerPixel) > 7) {
                currentBitOffset -= 8;
                currentByteIndex++;
            }

            // --- Write to Output (Part-Write Logic) ---

            int extractedIndex = extractedValue & pixelValueMask;

            if (texturePartMode == 0) {
                // Mode 0: Write to the high nibble (shift left by 4, e.g., for 4bpp)
                outputIndices[i] = (byte)(extractedIndex << 4);
            } else {
                // Mode 1: Write to the low nibble. Use bitwise OR to combine with the existing high nibble.
                outputIndices[i] = (byte)(outputIndices[i] | extractedIndex);
            }
        }
    }
}
