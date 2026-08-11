/**
 * Runtime language facade for all user-facing text.
 *
 * Menu arrays keep stable identities so a live language change can repaint an
 * already-open menu without leaving stale references behind.
 */
public final class TextStrings {

    public static final byte LANGUAGE_ENGLISH = 0;
    public static final byte LANGUAGE_RUSSIAN = 1;

    public static String FIND_THE_WALL_I_TOLD_YOU_AND_BLOW_IT_UP;
    public static String GO_GET_THE_DYNAMITE;
    public static String TO_CHANGE_WEAPON_PRESS_3;
    public static String PRESS_1_TO_OPEN_THE_DOOR;
    public static String PRESS_1_TO_MOVE_THE_LIFT;
    public static String WE_LL_NEED_SOME_DYNAMITE_MAYBE_I_SHOULD_LOOK_FOR_SOME;
    public static String OOPS_I_NEED_ANOTHER_KEY;
    public static String OH_I_NEED_A_KEY;
    public static String I_THINK_THAT_S_THE_WALL_SHE_MENTIONED;
    public static String GET_THE_SNIPER_RIFLE;
    public static String CHANGE;
    public static String YES;
    public static String NO;
    public static String SELECT;
    public static String MISSION_FAILED_GAME_OVER;
    public static String PAUSE;
    public static String RESUME;
    public static String BACK;
    public static String I_D_BETTER_USE_IT_TO_FINISH_MY_MISSION;
    public static String UNAVAILABLE;
    public static String SOUND;
    public static String HELP;
    public static String ABOUT;
    public static String MUSIC;
    public static String VIBRATION;
    public static String FLOORS;
    public static String SKY;
    public static String MUZZLE_LIGHT;
    public static String SCREEN_EFFECTS;
    public static String LANGUAGE;
    public static String ENGLISH;
    public static String RUSSIAN;
    public static String TEXTURED;
    public static String FLAT;
    public static String SOLID;
    public static String EMPTY_SPACE;
    public static String SETTINGS;
    public static String PLEASE_WAIT;
    public static String ON;
    public static String OFF;
    public static String QUIT;
    static String GO_ANNA;

    static final String[] mainMenuItems = new String[5];
    static final String[] pauseMenuItems = new String[6];
    static final String[] difficultyMenuItems = new String[6];
    static final String[] CHAPTER_MENU_DATA = new String[11];
    static final String[] CONFIRMATION_MENU_ITEMS = new String[3];
    static final String[] HELP_MENU_ITEMS = new String[12];
    static String[] ABOUT_MENU_TEXT;
    static String[][] storyText;

    private static byte currentLanguage;

    private static final String[] EN_MAIN_MENU = new String[]{
            "new game", "settings", "help", "about", "quit"
    };
    private static final String[] RU_MAIN_MENU = new String[]{
            "новая игра", "настройки", "справка", "об игре", "выход"
    };
    private static final String[] EN_PAUSE_MENU = new String[]{
            "resume", "new game", "settings", "help", "about", "quit"
    };
    private static final String[] RU_PAUSE_MENU = new String[]{
            "продолжить", "новая игра", "настройки", "справка", "об игре", "выход"
    };
    private static final String[] EN_DIFFICULTY = new String[]{
            "difficulty", "", "easy", "normal", "hard", "back"
    };
    private static final String[] RU_DIFFICULTY = new String[]{
            "сложность", "", "лёгкая", "обычная", "тяжёлая", "назад"
    };
    private static final String[] EN_CHAPTERS = new String[]{
            "chapter", "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "back"
    };
    private static final String[] RU_CHAPTERS = new String[]{
            "глава", "", "первая", "вторая", "третья", "четвёртая", "пятая", "шестая", "седьмая", "восьмая", "девятая", "назад"
    };
    private static final String[] EN_CONFIRMATION = new String[]{"are you sure?", "", "no"};
    private static final String[] RU_CONFIRMATION = new String[]{"вы уверены?", "", "нет"};
    private static final String[] EN_HELP = new String[]{
            "Controls:", "", "2/up - walk forward", "8/down - step backwards",
            "4/left - turn left", "6/right - turn right", "7 - strafe left",
            "9 - strafe right", "5/action - fire", "1 - open door/move lift",
            "3 - select weapon", "0 - toggle map"
    };
    private static final String[] RU_HELP = new String[]{
            "Управление:", "", "2/вверх - идти вперёд", "8/вниз - шаг назад",
            "4/влево - поворот влево", "6/вправо - поворот вправо", "7 - шаг влево",
            "9 - шаг вправо", "5/действие - огонь", "1 - дверь или лифт",
            "3 - выбрать оружие", "0 - карта"
    };

    private static final String[] EN_ABOUT = new String[]{
            "Covert Ops 3D", "", "Developed by:", "Micazook Mobile Ltd.", "",
            "Executive producers:", "Marcin Kochanowski", "Wojciech Charysz", "Michael Fotoohi", "",
            "Senior developer:", "Tomasz Mroczek", "", "Level design:", "Kamil Bachminski", "",
            "Texture artists:", "Kamil Bachminski", "Patryk Piescinski", "", "Character design:",
            "Lukasz 'Slizgi' Sliwinski", "Kamil Bachminski", "", "Music:", "Slawomir Opalinski", "",
            "Sound effects:", "Kamil Bachminski", "", "", "Publisher:", "Micazook Ltd.", "",
            "www.micazook.com", "", "for support email", "support@micazook.com", "",
            "(c) 2006 Micazook Ltd.", "Trademarks belong to", "their respective owners.", "",
            "All rights reserved!", "", "Covert Ops 3D RE project", "", "2025 AE-MODS.RU"
    };
    private static final String[] RU_ABOUT = new String[]{
            "Covert Ops 3D", "", "Разработчик:", "Micazook Mobile Ltd.", "",
            "Исполнительные продюсеры:", "Marcin Kochanowski", "Wojciech Charysz", "Michael Fotoohi", "",
            "Главный разработчик:", "Tomasz Mroczek", "", "Дизайн уровней:", "Kamil Bachminski", "",
            "Художники текстур:", "Kamil Bachminski", "Patryk Piescinski", "", "Дизайн персонажей:",
            "Lukasz 'Slizgi' Sliwinski", "Kamil Bachminski", "", "Музыка:", "Slawomir Opalinski", "",
            "Звуковые эффекты:", "Kamil Bachminski", "", "", "Издатель:", "Micazook Ltd.", "",
            "www.micazook.com", "", "Поддержка:", "support@micazook.com", "",
            "(c) 2006 Micazook Ltd.", "Торговые марки принадлежат", "их законным владельцам.", "",
            "Все права защищены.", "", "Проект Covert Ops 3D RE", "", "2025 AE-MODS.RU"
    };

    private static final String[][] EN_STORY = new String[][]{
        {
            "RMy name is captain Thomas Reed. My mission, Covert Operations in service of the US army. I began my career in Spain and since then I have participated in many secret missions against the enemy. As an Allied secret agent my job is to infiltrate and sabotage behind the enemy lines. This mission is a typical one, dangerous with my name written on it!! Our spy planes have revealed photographs of what seems to be of an immense constructions project taking place around the Weissberg Mountain in the German Alps. It seems that the Germans are digging a network of reinforcements and underground bunkers on a previously unparalleled scale. My mission is to hide aboard a transportation supply train until it gets to Weissberg and to meet with our undercover agent on location there. With her help, I need to get the documents that reveals the purpose of this enterprise which incidentally the HQ nicknamed Fort Weissberg. Not sure how yet but I need to devise a way to sabotage the railway system and other important installations on site. I need some luck today and lots of it!!"
        },
        {
            "ACaptain Reed I presume?", "RWho are you!?", "AAnna Sierck, MI5. I was told to meet you here in Weissberg.",
            "RSo why now, why in the train? If the Germans find us here, my mission is over!", "AThere has been a change of plans. Don't worry. We are safe, at least until the train stops.",
            "RWhat's happening, has something gone wrong?", "ANo don't worry, HQ's idea, just some last minute changes to keep the Germans guessing.",
            "RI knew it.", "AEver since our last failed attempt the trains are heavily searched. You'll have to leave at the last hidden station in the forest and walk to the fort by foot.",
            "RLast, failed attempt?? How many times have you tried so far?", "ARight now you don't need to know that. Ah and one more thing. The forts gates are not as heavily guarded as the train yard, but nevertheless you can expect a lot of resistance there. I know there is a sniper rifle stored somewhere at the station. Find it and use it against the gate guards."
        },
        {
            "AI see you found this rifle, good. You'll have to shoot the guards before entering Fort Weissberg.", "RAnd you?", "AI'll meet you inside. Maybe I'll be able to get a uniform for you.",
            "RThanks.", "AReed?", "RYes?", "AGood luck."
        },
        {
            "AGlad you made it.", "RPiece of cake.", "AUnfortunately I have some bad news. There's gossip of some sort of a secret weapon undergoing tests here. I don't know if it's true, but the guard outpost's has been heavily reinforced. Uniform will not do you any good - they are checking everyone's id cards now practically on every corridor.",
            "RWhat do I do then?", "AYou can get deeper into the fortress through the unfinished tunnels. But you'll need explosives, as some of the passages are systematically being sealed for security reasons. I'm sure there's dynamite somewhere here. Get it and then find a wall that looks like it shouldn't be there...", "RWhat? Can't you be a little bit more precise?", "AUnfortunately our plans backfired and we couldn't get you any uniforms. Sorry bad luck old chap.", "RAll right, but this my life on the line here."
        },
        {
            "AYou did it! Now all you need to do is to find the documents. I suppose they are locked somewhere in this level, perhaps you'll need to search for keys.", "RAnd what about you?", "AThere is some commotion in the base, I'll try and see what's going on. We will meet here after we're done.", "RSee you then."
        },
        {
            "RI've got these papers. Can we finally blow this place up? It's giving me the creeps.", "AI'm sorry, but there's been a slight change of plans again.", "RGreat. I was longing to hear it. What's happening now?", "AHave you ever heard of Clint Miller?", "RDoctor Clint Miller? The Nobel prize winner?", "AYes That's him. A few weeks ago he disappeared from his house in Boston. He's here now, arrived today. The Germans have kidnapped him.", "RWhat? Why?", "AFrom what I know he was conducting some sort of research on the possible military uses of sound waves back in the US.", "RYou mean...", "AOuch, sonic weapons. Germans are doing similar experiments but with no success, so far at least.", "RHe must have cracked it if the German's risked kidnapping him in the states.", "AThat's what I'm afraid of. We have to get him out of here and fast.", "RWhere do they keep him.", "AOn this very level. That's where you come in. Once again you'll need to use your sniper rifle and get rid of the guards.", "RYou lead the way."
        },
        {
            "RWhat now?", "AThese are the labs and prison's. Miller will be somewhere here. Be careful, I know we are past the outpost, but there can be some more soldiers wandering around.", "RDon't worry and wait here. I'll find him in no time."
        },
        {
            "ADoctor Miller? We're here to help you!", "MHelp?", "RYeah, to get you out of this prison and out of this country.", "MAh, prison. Yes, yes. What's your plan?", "RAnna?", "AUmm...", "MYou have came to rescue me without any plans??", "AWe didn't know you were going to be here.", "MMy goodness! Listen to me then: the only way to get out of here safely is by taking the train back out of here. They don't seem to care about guarding out bound trains from here.", "RHow do you know?", "MObservation young man, observation. There is no science without it.", "AOK lets make a move quickly.", "MPerhaps your big friend could find some explosives if we want to make sure no one comes after us?", "AGood idea, we were about to destroy this place anyway. Go, Reed, we'll meet at the train yard.", "RSure?", "AGo, go. We can't stick around here forever."
        },
        {
            "RAnna! What happened?!", "AI... I should have known that...", "RWhy oh why... don't talk too much.", "AMiller... He wasn't kidnapped... at all...", "RWhat? What are you saying?", "AIt was a trap... I don't know how to tell you but Miller is one of them. He came to Germany on his own accord?", "RMiller is a Nazi?", "AYes, he lured me... into this deceitful trap...", "RHe'll pay for that!", "ANo! You have to finish your mission. Set the dynamite... lets get the hell out of here...", "RNo, I won't leave it like that. Just... Anna?", "A...", "RHe'll pay. He'll pay good."
        },
        {
            "RSo, Fort Weissberg ended up being the biggest firework I've ever seen. Soon I will board this train and head for Switzerland. I will cross the Alps by foot and, play hide and seek with German soldiers before I get there, but that's another story. Works of Clint Miller lie buried deep in the heart of the Weissberg mountain, and of course the Nazis will never finish their sonic super weapon. Miller's ties to Third Reich were never be revealed and his mysterious disappearance is still a base for numerous theories and speculations. And I? I remain on service."
        }
    };

    private static final String[][] RU_STORY = new String[][]{
        {
            "RМеня зовут капитан Томас Рид. Я служу в подразделении секретных операций армии США. Моя карьера началась в Испании, и с тех пор я участвовал во множестве тайных миссий против врага. Как агент союзников, я должен проникать в тыл противника и устраивать диверсии. Эта миссия ничем не отличается - опасность буквально написана на моём имени. Наши разведсамолёты сфотографировали огромную стройку у горы Вайсберг в немецких Альпах. Похоже, немцы роют сеть укреплений и подземных бункеров невиданных масштабов. Я должен спрятаться в грузовом поезде, добраться до Вайсберга и встретиться там с нашим агентом. С её помощью мне нужно получить документы о цели этого проекта, который штаб окрестил фортом Вайсберг. Пока не знаю как, но придётся вывести из строя железную дорогу и другие важные объекты. Мне сегодня очень понадобится удача."
        },
        {
            "AКапитан Рид, полагаю?", "RКто вы?!", "AАнна Сирк, MI5. Мне приказали встретить вас здесь, в Вайсберге.",
            "RПочему именно сейчас, в поезде? Если немцы нас найдут, миссии конец!", "AПланы изменились. Не волнуйтесь, мы в безопасности, по крайней мере пока поезд не остановится.",
            "RЧто происходит? Что-то пошло не так?", "AНет, это идея штаба. Последние изменения, чтобы запутать немцев.",
            "RЯ так и знал.", "AПосле прошлой неудачной попытки поезда обыскивают особенно тщательно. Вам придётся выйти на последней скрытой станции в лесу и добираться до форта пешком.",
            "RПрошлая неудачная попытка? Сколько же их было?", "AСейчас вам этого знать не нужно. И ещё: ворота форта охраняют не так тщательно, как железнодорожный двор, но сопротивление будет серьёзным. Где-то на станции хранится снайперская винтовка. Найдите её и уберите охранников у ворот."
        },
        {
            "AВижу, винтовку вы нашли. Теперь нужно застрелить охранников перед входом в форт.", "RА вы?", "AВстречу вас внутри. Может, успею раздобыть для вас форму.",
            "RСпасибо.", "AРид?", "RДа?", "AУдачи."
        },
        {
            "AРада, что вы добрались.", "RПустяки.", "AК сожалению, у меня плохие новости. Ходят слухи, что здесь испытывают какое-то секретное оружие. Не знаю, правда ли это, но охранный пост серьёзно усилили. Форма вам не поможет - теперь почти в каждом коридоре проверяют удостоверения.",
            "RИ что мне делать?", "AВ глубь крепости можно попасть через недостроенные туннели. Но понадобятся взрывчатка: некоторые проходы намеренно заделывают для безопасности. Где-то здесь должен быть динамит. Найдите его, а потом стену, которой вроде бы не должно быть...", "RЧто? Нельзя ли поточнее?", "AНаши планы сорвались, и форму для вас достать не удалось. Простите, старина.", "RЛадно, на кону моя жизнь."
        },
        {
            "AВы справились! Теперь остаётся найти документы. Думаю, они заперты где-то на этом уровне, возможно, придётся поискать ключи.", "RА что будете делать вы?", "AНа базе переполох, попробую узнать, что происходит. Встретимся здесь, когда закончим.", "RДо встречи."
        },
        {
            "RДокументы у меня. Может, наконец взорвём это место? От него мурашки по коже.", "AПрости, но планы снова немного изменились.", "RПрекрасно. Именно это я и хотел услышать. Что случилось?", "AВы слышали о Клинте Миллере?", "RО докторе Клинте Миллере, нобелевском лауреате?", "AДа, о нём. Несколько недель назад он исчез из дома в Бостоне. Сегодня он прибыл сюда. Немцы похитили его.", "RЧто? Зачем?", "AНасколько мне известно, в США он исследовал возможное военное применение звуковых волн.", "RВы хотите сказать...", "AДа, звуковое оружие. Немцы проводят похожие опыты, но пока без успеха.", "RЕсли они рискнули похитить его в Штатах, значит, он разгадал задачу.", "AИменно этого я боюсь. Нужно вытащить его как можно скорее.", "RГде его держат?", "AНа этом самом уровне. И тут снова нужны вы: возьмите снайперскую винтовку и устраните охранников.", "RВедите."
        },
        {
            "RЧто теперь?", "AЗдесь лаборатории и тюрьма. Миллер должен быть где-то рядом. Будьте осторожны: мы миновали пост, но вокруг ещё могут бродить солдаты.", "RНе волнуйтесь, ждите здесь. Я быстро его найду."
        },
        {
            "AДоктор Миллер? Мы пришли помочь вам!", "MПомочь?", "RДа, вывести вас из тюрьмы и из этой страны.", "MАх, тюрьма. Да, да. И каков ваш план?", "RАнна?", "AЭ-э...", "MВы пришли меня спасать совсем без плана?", "AМы не знали, что вы будете здесь.", "MБоже мой. Тогда слушайте: единственный безопасный путь - уехать отсюда на поезде. Похоже, уходящие поезда они не охраняют.", "RОткуда вы знаете?", "MНаблюдение, молодой человек. Без него нет науки.", "AХорошо, давайте двигаться быстрее.", "MВозможно, ваш крепкий друг найдёт взрывчатку, если мы хотим быть уверены, что за нами никто не пойдёт.", "AХорошая мысль, мы и так собирались уничтожить это место. Идите, Рид, встретимся на станции.", "RУверены?", "AИдите же. Нельзя торчать здесь вечно."
        },
        {
            "RАнна! Что случилось?!", "AЯ... должна была догадаться...", "RПочему... не тратьте силы на разговоры.", "AМиллер... его вовсе не похищали...", "RЧто? Что вы говорите?", "AЭто была ловушка... Не знаю, как сказать, но Миллер один из них. Он сам приехал в Германию.", "RМиллер - нацист?", "AДа, он заманил меня... в эту мерзкую ловушку...", "RОн за это заплатит!", "AНет! Вы должны закончить миссию. Установите динамит... и уходите отсюда...", "RНет, я не брошу всё так. Просто... Анна?", "A...", "RОн заплатит. Сполна."
        },
        {
            "RФорт Вайсберг стал самым большим фейерверком, который я когда-либо видел. Скоро я сяду на поезд в Швейцарию. Потом перейду Альпы пешком и поиграю в прятки с немецкими солдатами - но это уже другая история. Работы Клинта Миллера лежат погребёнными глубоко в сердце горы Вайсберг, и нацисты уже никогда не закончат своё звуковое супероружие. Связи Миллера с Третьим рейхом так и не будут раскрыты, а его загадочное исчезновение останется основой для множества теорий. А я? Я остаюсь на службе."
        }
    };

    static {
        setLanguage(LANGUAGE_ENGLISH);
    }

    private TextStrings() {
    }

    public static void setLanguage(byte requestedLanguage) {
        if (requestedLanguage == LANGUAGE_RUSSIAN) {
            currentLanguage = LANGUAGE_RUSSIAN;
            applyRussian();
        } else {
            currentLanguage = LANGUAGE_ENGLISH;
            applyEnglish();
        }
    }

    public static byte getLanguage() {
        return currentLanguage;
    }

    public static String getFontConfigPath() {
        return currentLanguage == LANGUAGE_RUSSIAN
                ? "/gamedata/font/ru_font.txt" : "/gamedata/font/en_font.txt";
    }

    public static String getLanguageName() {
        return currentLanguage == LANGUAGE_RUSSIAN ? RUSSIAN : ENGLISH;
    }

    private static void applyEnglish() {
        FIND_THE_WALL_I_TOLD_YOU_AND_BLOW_IT_UP = "find the wall i told you|and blow it up!";
        GO_GET_THE_DYNAMITE = "go, get the dynamite!";
        TO_CHANGE_WEAPON_PRESS_3 = "to change weapon press 3";
        PRESS_1_TO_OPEN_THE_DOOR = "press 1 to open the door";
        PRESS_1_TO_MOVE_THE_LIFT = "press 1 to move the lift";
        WE_LL_NEED_SOME_DYNAMITE_MAYBE_I_SHOULD_LOOK_FOR_SOME = "we'll need some dynamite|maybe i should look for some";
        OOPS_I_NEED_ANOTHER_KEY = "oops, i need another key...";
        OH_I_NEED_A_KEY = "oh, i need a key...";
        I_THINK_THAT_S_THE_WALL_SHE_MENTIONED = "i think that's the wall|she mentioned";
        GET_THE_SNIPER_RIFLE = "get the sniper rifle!";
        CHANGE = "change";
        YES = "yes";
        NO = "no";
        SELECT = "select";
        MISSION_FAILED_GAME_OVER = "mission failed|game over";
        PAUSE = "pause";
        RESUME = "resume";
        BACK = "back";
        I_D_BETTER_USE_IT_TO_FINISH_MY_MISSION = "i'd better use it|to finish my mission";
        UNAVAILABLE = "unavailable";
        SOUND = "sound: ";
        HELP = "help";
        ABOUT = "about";
        MUSIC = "music: ";
        VIBRATION = "vibration: ";
        FLOORS = "floors: ";
        SKY = "sky: ";
        MUZZLE_LIGHT = "muzzle light: ";
        SCREEN_EFFECTS = "screen fx: ";
        LANGUAGE = "language: ";
        ENGLISH = "english";
        RUSSIAN = "russian";
        TEXTURED = "textured";
        FLAT = "flat";
        SOLID = "solid";
        EMPTY_SPACE = "";
        SETTINGS = "settings";
        PLEASE_WAIT = "please wait...";
        ON = "on";
        OFF = "off";
        QUIT = "quit";
        GO_ANNA = "go now to the agent anna";

        copy(mainMenuItems, EN_MAIN_MENU);
        copy(pauseMenuItems, EN_PAUSE_MENU);
        copy(difficultyMenuItems, EN_DIFFICULTY);
        copy(CHAPTER_MENU_DATA, EN_CHAPTERS);
        copy(CONFIRMATION_MENU_ITEMS, EN_CONFIRMATION);
        copy(HELP_MENU_ITEMS, EN_HELP);
        ABOUT_MENU_TEXT = EN_ABOUT;
        storyText = EN_STORY;
    }

    private static void applyRussian() {
        FIND_THE_WALL_I_TOLD_YOU_AND_BLOW_IT_UP = "найдите стену, о которой|я говорила, и взорвите её!";
        GO_GET_THE_DYNAMITE = "найдите динамит!";
        TO_CHANGE_WEAPON_PRESS_3 = "для смены оружия нажмите 3";
        PRESS_1_TO_OPEN_THE_DOOR = "нажмите 1, чтобы открыть дверь";
        PRESS_1_TO_MOVE_THE_LIFT = "нажмите 1, чтобы вызвать лифт";
        WE_LL_NEED_SOME_DYNAMITE_MAYBE_I_SHOULD_LOOK_FOR_SOME = "нужен динамит.|надо его поискать";
        OOPS_I_NEED_ANOTHER_KEY = "упс, нужен другой ключ...";
        OH_I_NEED_A_KEY = "нужен ключ...";
        I_THINK_THAT_S_THE_WALL_SHE_MENTIONED = "думаю, это та самая|стена, о которой она говорила";
        GET_THE_SNIPER_RIFLE = "найдите снайперскую винтовку";
        CHANGE = "изменить";
        YES = "да";
        NO = "нет";
        SELECT = "выбрать";
        MISSION_FAILED_GAME_OVER = "миссия провалена|игра окончена";
        PAUSE = "пауза";
        RESUME = "продолжить";
        BACK = "назад";
        I_D_BETTER_USE_IT_TO_FINISH_MY_MISSION = "лучше приберегу это|для завершения миссии";
        UNAVAILABLE = "недоступно";
        SOUND = "звук: ";
        HELP = "справка";
        ABOUT = "об игре";
        MUSIC = "музыка: ";
        VIBRATION = "вибрация: ";
        FLOORS = "полы: ";
        SKY = "небо: ";
        MUZZLE_LIGHT = "вспышка: ";
        SCREEN_EFFECTS = "эффекты: ";
        LANGUAGE = "язык: ";
        ENGLISH = "английский";
        RUSSIAN = "русский";
        TEXTURED = "текстуры";
        FLAT = "плоские";
        SOLID = "однотонное";
        EMPTY_SPACE = "";
        SETTINGS = "настройки";
        PLEASE_WAIT = "подождите...";
        ON = "вкл";
        OFF = "выкл";
        QUIT = "выход";
        GO_ANNA = "идите к агенту Анне";

        copy(mainMenuItems, RU_MAIN_MENU);
        copy(pauseMenuItems, RU_PAUSE_MENU);
        copy(difficultyMenuItems, RU_DIFFICULTY);
        copy(CHAPTER_MENU_DATA, RU_CHAPTERS);
        copy(CONFIRMATION_MENU_ITEMS, RU_CONFIRMATION);
        copy(HELP_MENU_ITEMS, RU_HELP);
        ABOUT_MENU_TEXT = RU_ABOUT;
        storyText = RU_STORY;
    }

    private static void copy(String[] target, String[] source) {
        System.arraycopy(source, 0, target, 0, target.length);
    }
}
