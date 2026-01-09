import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class WeaponManager {

    // ==================== Оружие ====================
    private Weapon[] weapons;
    private int currentWeaponId;
    private int pendingWeaponId;

    // ==================== Анимация ====================
    private boolean switchAnimationActive;
    private int animationState;
    private int spriteFrame;

    // ==================== Таймер ====================
    private int cooldownTimer;

    private static final int SWITCH_FRAMES = 8;

    // ==================== Конструктор ====================

    public WeaponManager() {
        weapons = WeaponFactory.createAllWeapons();
        currentWeaponId = WeaponFactory.FIST;
        pendingWeaponId = WeaponFactory.FIST;
        switchAnimationActive = false;
        animationState = 0;
        spriteFrame = 0;
        cooldownTimer = -32768;
    }

    public void initialize() {
        weapons[currentWeaponId].loadSprites();
    }

    // ==================== Геттеры ====================

    public Weapon getCurrentWeapon() {
        return weapons[currentWeaponId];
    }

    public int getCurrentWeaponId() {
        return currentWeaponId;
    }

    public Weapon getWeapon(int id) {
        if (id >= 0 && id < weapons.length) {
            return weapons[id];
        }
        return null;
    }

    public boolean isSwitchAnimationActive() {
        return switchAnimationActive;
    }

    public int getAnimationState() {
        return animationState;
    }

    public int getSpriteFrame() {
        return spriteFrame;
    }

    public int getCooldownTimer() {
        return cooldownTimer;
    }

    public void setSpriteFrame(int frame) {
        spriteFrame = frame;
    }

    public void setCooldownTimer(int timer) {
        cooldownTimer = timer;
    }

    // ==================== Переключение ====================

    public void switchToNext(int[] ammoCounts, boolean[] weaponsAvailable) {
        if (switchAnimationActive) {
            return;
        }

        int nextId = findNextWeapon(currentWeaponId, ammoCounts, weaponsAvailable);
        if (nextId != currentWeaponId) {
            pendingWeaponId = nextId;
            startSwitch();
        }
    }

    public void forceSwitch(int weaponId, int[] ammoCounts, boolean[] weaponsAvailable) {
        if (switchAnimationActive) {
            return;
        }
        if (weaponId == currentWeaponId) {
            return;
        }
        if (!isWeaponAvailable(weaponId, ammoCounts, weaponsAvailable)) {
            return;
        }

        pendingWeaponId = weaponId;
        startSwitch();
    }

    private void startSwitch() {
        switchAnimationActive = true;
        animationState = SWITCH_FRAMES;
    }

    private int findNextWeapon(int fromId, int[] ammoCounts, boolean[] weaponsAvailable) {
        int nextId = fromId;
        for (int i = 0; i < weapons.length; i++) {
            nextId = (nextId + 1) % weapons.length;
            if (isWeaponAvailable(nextId, ammoCounts, weaponsAvailable)) {
                return nextId;
            }
        }
        return fromId;
    }

    public int findAvailableWeapon(int fromId, int[] ammoCounts, boolean[] weaponsAvailable) {
        if (isWeaponAvailable(fromId, ammoCounts, weaponsAvailable)) {
            return fromId;
        }

        for (int i = 0; i < weapons.length; i++) {
            int checkId = (fromId + i) % weapons.length;
            if (isWeaponAvailable(checkId, ammoCounts, weaponsAvailable)) {
                return checkId;
            }
        }

        return WeaponFactory.FIST;
    }

    private boolean isWeaponAvailable(int weaponId, int[] ammoCounts, boolean[] weaponsAvailable) {
        if (weaponId < 0 || weaponId >= weapons.length) {
            return false;
        }
        if (!weaponsAvailable[weaponId]) {
            return false;
        }

        Weapon weapon = weapons[weaponId];

        if (!weapon.requiresAmmo()) {
            return true;
        }

        int ammoType = weapon.getEffectiveAmmoType();
        return ammoCounts[ammoType] > 0;
    }

    // ==================== Обновление ====================

    public void update(int[] ammoCounts, boolean[] weaponsAvailable) {
        if (cooldownTimer > -32768) {
            cooldownTimer--;
        }

        if (switchAnimationActive) {
            updateSwitchAnimation();
        }

        if (!switchAnimationActive) {
            int nextAvailable = findAvailableWeapon(currentWeaponId, ammoCounts, weaponsAvailable);
            if (nextAvailable != currentWeaponId) {
                pendingWeaponId = nextAvailable;
                startSwitch();
            }
        }
    }

    private void updateSwitchAnimation() {
        animationState--;

        if (animationState == 0) {
            weapons[currentWeaponId].unloadSprites();
            currentWeaponId = pendingWeaponId;
            weapons[currentWeaponId].loadSprites();
            spriteFrame = 0;
        }

        if (animationState == -SWITCH_FRAMES) {
            switchAnimationActive = false;
            animationState = 0;
        }
    }

    // ==================== Стрельба ====================

    public boolean canFire(int[] ammoCounts, int difficulty) {
        if (switchAnimationActive) {
            return false;
        }

        Weapon weapon = getCurrentWeapon();
        int requiredCooldown = -weapon.getCooldown(difficulty);

        if (weapon.getIsAutomatic()) {
            if (cooldownTimer > 0) {
                return false;
            }
        } else {
            if (cooldownTimer >= requiredCooldown) {
                return false;
            }
        }

        if (weapon.requiresAmmo()) {
            int ammoType = weapon.getEffectiveAmmoType();
            if (ammoCounts[ammoType] <= 0) {
                return false;
            }
        }

        return true;
    }

    public boolean fire(int[] ammoCounts, boolean[] weaponsAvailable, int difficulty, int levelId, int sectorType) {
        if (!canFire(ammoCounts, difficulty)) {
            return false;
        }

        Weapon weapon = getCurrentWeapon();

        if (weapon.getIsThrowable()) {
            return fireThrowable(ammoCounts, weaponsAvailable, levelId, sectorType);
        }

        if (weapon.getIsAutomatic()) {
            return fireAutomatic(ammoCounts, difficulty);
        }

        return fireStandard(ammoCounts, weaponsAvailable, difficulty);
    }

    private boolean fireStandard(int[] ammoCounts, boolean[] weaponsAvailable, int difficulty) {
        Weapon weapon = getCurrentWeapon();

        if (weapon.requiresAmmo()) {
            int ammoType = weapon.getEffectiveAmmoType();
            ammoCounts[ammoType]--;
        }

        LevelLoader.gameWorld.fireWeapon();
        spriteFrame = 1;
        cooldownTimer = 1;

        if (weapon.getConsumeOnUse() && !weapon.getIsThrowable()) {
            // Panzerfaust - переключается после выстрела в releaseFire
        }

        return true;
    }

    private boolean fireAutomatic(int[] ammoCounts, int difficulty) {
        Weapon weapon = getCurrentWeapon();

        if (spriteFrame == 0) {
            int ammoType = weapon.getEffectiveAmmoType();
            ammoCounts[ammoType]--;
            LevelLoader.gameWorld.fireWeapon();
            spriteFrame = 1;
            cooldownTimer = 1;
        } else {
            spriteFrame = 0;
            cooldownTimer = weapon.getCooldown(difficulty);
        }

        return true;
    }

    private boolean fireThrowable(int[] ammoCounts, boolean[] weaponsAvailable, int levelId, int sectorType) {
        Weapon weapon = getCurrentWeapon();
        int ammoType = weapon.getEffectiveAmmoType();

        // Проверка для миссии
        if ((levelId == 4 || levelId == 7 || levelId == 8) && ammoCounts[ammoType] == 1) {
            if (levelId != 4 || sectorType != 666) {
                GameEngine.messageText = TextStrings.I_D_BETTER_USE_IT_TO_FINISH_MY_MISSION;
                GameEngine.messageTimer = 50;
                return false;
            }
        }

        if (LevelLoader.gameWorld.throwGrenade()) {
            ammoCounts[ammoType]--;
            cooldownTimer = 0;
            pendingWeaponId = findAvailableWeapon(currentWeaponId, ammoCounts, weaponsAvailable);
            startSwitch();
            return true;
        }

        return false;
    }

    public void releaseFire(int[] ammoCounts, boolean[] weaponsAvailable, int difficulty) {
        Weapon weapon = getCurrentWeapon();

        // Reset sprite frame when cooldown expires for non-automatic weapons
        if (cooldownTimer <= 0) {
            if (weapon.getIsAutomatic()) {
                // Automatic weapons reset when fire is released
                spriteFrame = 0;
            } else {
                // Non-automatic weapons reset after cooldown
                spriteFrame = 0;
            }
        }

        // Panzerfaust special case - show empty tube frame then switch
        if (weapon.id == WeaponFactory.PANZERFAUST && spriteFrame == 1) {
            spriteFrame = 2;
            pendingWeaponId = findAvailableWeapon(WeaponFactory.PANZERFAUST, ammoCounts, weaponsAvailable);
            startSwitch();
        }
    }
    public void forceWeaponSwitch(int weaponId) {
        if (switchAnimationActive) {
            return;
        }
        if (weaponId < 0 || weaponId >= weapons.length) {
            return;
        }

        pendingWeaponId = weaponId;
        startSwitch();
    }

    // ==================== Рендеринг ====================

    public void render(Graphics graphics, int headBob) {
        Weapon weapon = getCurrentWeapon();
        Image sprite = weapon.getSprite(spriteFrame);

        if (sprite == null) {
            return;
        }

        int weaponHeight = sprite.getHeight();

        if (switchAnimationActive) {
            int animState = animationState;
            if (animState < 0) {
                animState = -animState;
            }
            weaponHeight = (weaponHeight * animState) >> 3;
        }

        int weaponX;
        if (weapon.isCentered()) {
            weaponX = (PortalRenderer.VIEWPORT_WIDTH - sprite.getWidth()) / 2;
        } else {
            weaponX = PortalRenderer.VIEWPORT_WIDTH - sprite.getWidth();
        }

        int weaponY = PortalRenderer.VIEWPORT_HEIGHT - weaponHeight - headBob + 3;

        graphics.drawImage(sprite, weaponX, weaponY, 0);
    }

    public int getDisplayAmmoType() {
        Weapon weapon = getCurrentWeapon();
        if (weapon.id == WeaponFactory.RIFLE || weapon.id == WeaponFactory.STEN) {
            return WeaponFactory.LUGER;
        }
        return weapon.getAmmoType();
    }
}