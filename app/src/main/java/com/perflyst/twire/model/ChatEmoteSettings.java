package com.perflyst.twire.model;

public class ChatEmoteSettings {
    private boolean bttv_enabled;
    private boolean ffz_enabled;
    private boolean seventv_enabled;

    public ChatEmoteSettings(boolean bttv_enabled, boolean ffz_enabled, boolean seventv_enabled) {
        this.bttv_enabled = bttv_enabled;
        this.ffz_enabled = ffz_enabled;
        this.seventv_enabled = seventv_enabled;
    }

    public boolean isBBTVenabled() {
        return bttv_enabled;
    }
    public boolean isFFZenabled() {
        return ffz_enabled;
    }
    public boolean isSevenTVenabled() {
        return seventv_enabled;
    }
}
