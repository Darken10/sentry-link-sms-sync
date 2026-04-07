package com.africasys.sentrylink;

import android.os.Bundle;
import android.widget.TextView;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * CaptureActivity forcé en portrait avec overlay d'étape.
 * L'orientation est définie dans AndroidManifest.xml (screenOrientation="portrait").
 */
public class PortraitCaptureActivity extends CaptureActivity {

    /** Étape courante du wizard, à définir avant de lancer le scanner. */
    public static int scanStep = 1;

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_scan_capture);
        return (DecoratedBarcodeView) findViewById(R.id.zxing_barcode_scanner);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateStepOverlay();
    }

    private void updateStepOverlay() {
        TextView dot1 = findViewById(R.id.scanStepDot1);
        TextView dot2 = findViewById(R.id.scanStepDot2);
        TextView dot3 = findViewById(R.id.scanStepDot3);
        TextView stepLabel = findViewById(R.id.scanStepLabel);
        TextView stepTitle = findViewById(R.id.scanStepTitle);

        if (dot1 == null || dot2 == null || dot3 == null) return;

        switch (scanStep) {
            case 1:
                dot1.setBackgroundResource(R.drawable.step_dot_active);
                dot2.setBackgroundResource(R.drawable.step_dot_inactive);
                dot3.setBackgroundResource(R.drawable.step_dot_inactive);
                if (stepLabel != null) stepLabel.setText("Étape 1 sur 3");
                if (stepTitle != null) stepTitle.setText("CONFIGURATION DE L'UNITÉ");
                break;
            case 2:
                dot1.setBackgroundResource(R.drawable.step_dot_done);
                dot2.setBackgroundResource(R.drawable.step_dot_active);
                dot3.setBackgroundResource(R.drawable.step_dot_inactive);
                if (stepLabel != null) stepLabel.setText("Étape 2 sur 3");
                if (stepTitle != null) stepTitle.setText("PROTOCOLE DE GROUPES");
                break;
            case 3:
                dot1.setBackgroundResource(R.drawable.step_dot_done);
                dot2.setBackgroundResource(R.drawable.step_dot_done);
                dot3.setBackgroundResource(R.drawable.step_dot_active);
                if (stepLabel != null) stepLabel.setText("Étape 3 sur 3");
                if (stepTitle != null) stepTitle.setText("TUNNEL DE COMMUNICATION");
                break;
        }
    }
}
