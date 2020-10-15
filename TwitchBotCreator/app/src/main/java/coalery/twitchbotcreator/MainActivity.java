package coalery.twitchbotcreator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String BOT_FILE_PATH = "/twitchbot";
    private static final String BOT_FILE_NAME = "twitchbot.js";

    private TwitchBot twitchBot; // 봇 객체
    private boolean isBotOn; // 봇이 켜져있는가.

    private Button botSwitch; // 봇 스위치 버튼
    private EditText codeText; // 코드 텍스트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        isBotOn = false;

        // #region 봇 스위치 버튼 설정
        botSwitch = findViewById(R.id.bot_switch);
        botSwitch.setText("켜기");

        botSwitch.setOnClickListener((view) -> {
            if(isBotOn) { // 봇이 켜져있으면, 봇을 끈다.
                botOff();
            } else { // 봇이 꺼져있으면, 봇을 킨다.
                botOn();
            }
        });
        // #endregion

        // #region 저장 버튼 설정
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setText("저장");
        saveButton.setOnClickListener((view) -> saveFile());
        // #endregion

        // #region 코드 텍스트 설정
        String code = loadFile();
        codeText = findViewById(R.id.code_text);
        codeText.setText(code);
        Log.i("fpowaegj2@@@@@", code);
        // #endregion
    }

    private void botOff() {
        botSwitch.setEnabled(false); // 처리 중에는 버튼을 비활성화한다.
        if(twitchBot != null) { // 봇을 끈다.
            twitchBot.disconnect();
            twitchBot.dispose();
        }
        isBotOn = false;
        botSwitch.setText("켜기");
        botSwitch.setEnabled(true); // 처리를 완료하였으므로 버튼을 활성화시킨다.
        codeText.setEnabled(true); // 봇을 껐으므로, 코드 텍스트를 활성화시킨다.
    }

    private void botOn() {
        botSwitch.setEnabled(false); // 처리 중에는 버튼을 비활성화시킨다.
        codeText.setEnabled(false); // 봇을 켰으므로 코드 텍스트를 비활성화시킨다.
        BotInitializeThread botInitThread = new BotInitializeThread(codeText.getText().toString(), new BotInitializeCallback());
        botInitThread.start(); // 초기화 쓰레드를 시작한다.
    }

    private String getScriptFilePath() {
        String scriptFilePath = null;
        if(Build.VERSION.SDK_INT < 29) scriptFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + BOT_FILE_PATH;
        else {
            File dir = getApplicationContext().getExternalFilesDir(BOT_FILE_PATH);
            if(dir != null && dir.exists()) {
                scriptFilePath = dir.getAbsolutePath();
            }
        }
        return scriptFilePath;
    }

    private void writeDefaultFileIfNotExist(String scriptFilePath) throws IOException {
        File scriptFileDir = new File(scriptFilePath);
        if(!scriptFileDir.exists()) { scriptFileDir.mkdir(); } // 디렉토리가 없다면 만든다.

        File scriptFile = new File(scriptFilePath, BOT_FILE_NAME);
        if(!scriptFile.exists()) { // 파일이 없으면 만든다.
            String defaultCode =
                    "function onStart() {}\n" +
                    "\n" +
                    "function onMessageReceived(channel, badges, sender_id, sender_nickname, message) {\n" +
                    "    return message;\n" +
                    "}\n";

            String[] codeLines = defaultCode.split("\n");

            BufferedWriter bw = new BufferedWriter(new FileWriter(scriptFile, false));
            for(String codeLine : codeLines) {
                bw.write(codeLine);
                bw.newLine();
            }
            bw.close();
        }
    }

    private String loadFile() { // 파일을 읽어서 파일 내용을 반환한다.
        String result = "";
        try {
            String scriptFilePath = getScriptFilePath();
            writeDefaultFileIfNotExist(scriptFilePath);

            if(scriptFilePath == null) {
                Toast.makeText(getApplicationContext(), "파일 읽기를 실패하여 기본 코드로 대체됩니다.", Toast.LENGTH_LONG).show();
                return "function onStart() {}\n" +
                        "\n" +
                        "function onMessageReceived(channel, badges, sender_id, sender_nickname, message) {\n" +
                        "    return message;\n" +
                        "}\n";
            }

            BufferedReader br = new BufferedReader(new FileReader(new File(scriptFilePath, BOT_FILE_NAME)));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            result = sb.toString();
        } catch(IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return result;
    }

    private void saveFile() {
        try {
            String scriptFilePath = getScriptFilePath();

            File scriptFileDir = new File(scriptFilePath);
            if(!scriptFileDir.exists()) { scriptFileDir.mkdir(); } // 디렉토리가 없다면 만든다.

            File scriptFile = new File(scriptFilePath, BOT_FILE_NAME);
            String code = codeText.getText().toString();
            String[] codeLines = code.split("\n");

            BufferedWriter bw = new BufferedWriter(new FileWriter(scriptFile, false));
            for(String codeLine : codeLines) {
                bw.write(codeLine);
                bw.newLine();
            }
            bw.close();
        } catch(IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class BotInitializeCallback implements IBotInitializeCallback { // 봇 초기화가 완료되면 호출되는 콜백
        @Override
        public void onCompleted(TwitchBot bot) {
            MainActivity.this.twitchBot = bot;
            runOnUiThread(() -> { // 처리를 완료하였으므로 버튼을 다시 활성화시킨다.
                isBotOn = true;
                botSwitch.setText("끄기");
                botSwitch.setEnabled(true);
            });
        }
    }

}
