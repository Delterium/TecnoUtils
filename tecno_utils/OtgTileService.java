package xyz.delterium.tecno_utils;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class OtgTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean isEnabled = OtgUtils.isOtgEnabled();
        boolean newState = !isEnabled;
        OtgUtils.setOtgEnabled(newState);
        OtgUtils.updateOtgService(this, newState);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        
        boolean isEnabled = OtgUtils.isOtgEnabled();
        if (isEnabled) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_otg_on));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_otg_off));
        }
        tile.updateTile();
    }
}