package name.hampton.mike.nfctapexample;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NFCExampleActivity extends Activity {

  private NfcAdapter mAdapter;
  private PendingIntent mPendingIntent;
  private String TAG = "NFCExampleActivity";

  private static List<String> getPayload(NdefMessage msg) {
    List<String> ret = new ArrayList<>();
    for (final NdefRecord record : msg.getRecords()) {
      ret.add(record.toString());
    }
    return ret;
  }

  private String getHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte aByte : bytes) {
      int b = aByte & 0xff;
      if (b < 0x10) {
        sb.append('0');
      }
      sb.append(Integer.toHexString(b));
      sb.append(" ");
    }
    return sb.toString();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "In onCreate");
    setContentView(R.layout.activity_nfc);
    mAdapter = NfcAdapter.getDefaultAdapter(this);
    if (mAdapter == null) {
      Log.e(TAG, "No NFC, exiting");
      finish(Activity.RESULT_CANCELED, "NFC not enabled");
      return;
    }
    setContentView(R.layout.activity_nfc);
    mPendingIntent = PendingIntent.getActivity(this, 0,
        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
  }

  private void finish(int resultCode, String data) {
    Intent result = new Intent(this.getIntent().getAction());
    result.putExtra("PAYLOAD", data);
    this.setResult(resultCode, result);
    this.runOnUiThread(new Runnable() {
      public void run() {
        NFCExampleActivity.this.finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "In onResume");
    if (mAdapter != null) {
      if (!mAdapter.isEnabled()) {
        Log.e(TAG, "NFC not enabled");
        finish(RESULT_CANCELED, "NFC not enabled");
      }

      mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mAdapter != null && mAdapter.isEnabled()) {
      mAdapter.disableForegroundDispatch(this);
    }
  }

  private void resolveIntent(Intent intent) {
    NdefMessage[] msgs;
    String action = intent.getAction();
    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
        || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
        || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
      Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
      if (rawMsgs != null) {
        msgs = new NdefMessage[rawMsgs.length];
        for (int i = 0; i < rawMsgs.length; i++) {
          msgs[i] = (NdefMessage) rawMsgs[i];
        }
      } else {
        // Unknown tag type
        byte[] empty = new byte[0];
        byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        String payloadStr = stringifyTagData(tag);
        byte[] payload = payloadStr.getBytes();
        Log.d(TAG, String.format("stringifyTagData **************** %s ****************", payloadStr));
        NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
        msgs = new NdefMessage[]{msg};
      }
      String payload = null;
      for (NdefMessage msg : msgs) {
        payload = buildPayload(msg).toString();
      }

      Log.d(TAG, String.format("DATA **************** %s ****************", payload));

      finish(RESULT_OK, payload);
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    Log.d(TAG, "In onNewIntent");
    setIntent(intent);
    resolveIntent(intent);
  }

  private String stringifyTagData(Parcelable p) {
    Log.d(TAG, "In stringifyTagData");
    StringBuilder sb = new StringBuilder();
    Tag tag = (Tag) p;
    byte[] id = tag.getId();
    sb.append("Tag ID (hex): ").append(getHex(id)).append("\n");
    String prefix = "android.nfc.tech.";
    sb.append("Technologies: ");
    for (String tech : tag.getTechList()) {
      sb.append(tech.substring(prefix.length()));
      sb.append(", ");
    }
    sb.delete(sb.length() - 2, sb.length());
    for (String tech : tag.getTechList()) {
      if (tech.equals(MifareClassic.class.getName())) {
        sb.append('\n');
        // MIFARE is the NXP Semiconductors-owned trademark of a series of chips widely used in contactless smart cards and proximity cards.
        MifareClassic mifareTag = MifareClassic.get(tag);
        String type = "Unknown";
        switch (mifareTag.getType()) {
          case MifareClassic.TYPE_CLASSIC:
            type = "Classic";
            break;
          case MifareClassic.TYPE_PLUS:
            type = "Plus";
            break;
          case MifareClassic.TYPE_PRO:
            type = "Pro";
            break;
        }
        sb.append("Mifare Classic type: ");
        sb.append(type);
        sb.append('\n');
        sb.append("Mifare size: ");
        sb.append(mifareTag.getSize()).append(" bytes");
        sb.append('\n');
        sb.append("Mifare sectors: ");
        sb.append(mifareTag.getSectorCount());
        sb.append('\n');
        sb.append("Mifare blocks: ");
        sb.append(mifareTag.getBlockCount());
      }
      if (tech.equals(MifareUltralight.class.getName())) {
        sb.append('\n');
        MifareUltralight mifareUlTag = MifareUltralight.get(tag);
        String type = "Unknown";
        switch (mifareUlTag.getType()) {
          case MifareUltralight.TYPE_ULTRALIGHT:
            type = "Ultralight";
            break;
          case MifareUltralight.TYPE_ULTRALIGHT_C:
            type = "Ultralight C";
            break;
        }
        sb.append("Mifare Ultralight type: ");
        sb.append(type);
      }
    }
    return sb.toString();
  }

  private List<String> buildPayload(NdefMessage msg) {
    if (msg == null) { // || msg.length == 0) {
      return null;
    }
    return getPayload(msg);
  }
}
