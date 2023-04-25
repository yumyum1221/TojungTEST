package com.example.tojung;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    TextView chatting;  //chatgpt 대답
    TextView ques; //stt Text
    Button option_button;
    Intent intent;
    Button button; //음성 버튼
    SpeechRecognizer mRecognizer; //음성인식기 객체
    final int PERMISSION = 1;
    List<Message> messageList; //메시지를 저장하는 리스트
    MessageAdapter messageAdapter;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    private static final String MY_SECRET_KEY = "sk-";
    //!!!!!!!!!!!!!!!!!!!!!** API **!!!!!!!!!//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        option_button =(Button) findViewById(R.id.option_button); //옵션버튼
        ques = findViewById(R.id.ques); //질문
        chatting=findViewById(R.id.chatting); //봇대답
        button = findViewById(R.id.button); //음성 버튼

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);

        //RecognizerIntent 생성:: 음성인식 기능 인텐트 설정
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName()); //여분의 키
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR"); //언어 설정

        //안드로이드 버전 확인 후 음성 권한 체크
        if(Build.VERSION.SDK_INT >= 23){
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO},PERMISSION);
        }

        button.setOnClickListener(new View.OnClickListener(){ //녹음버튼 눌렀을 때!!!
            @Override
            public void onClick(View v){
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                //새 SpeechRecognizer를 만드는 팩토리 메서드
                mRecognizer.setRecognitionListener(listener); //리스너 설정
                mRecognizer.startListening(intent);

            }
        });


        option_button.setOnClickListener(new View.OnClickListener() {
            //옵션 버튼 클릭시 화면 이동
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), option.class);
                startActivity(intent);
            }
        });

        client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String chatLog = chatting.getText().toString();
                if(!chatLog.isEmpty()){
                    chatLog += "\n\n";
                }
                chatLog += sentBy + ": " + message;
                chatting.setText(chatLog);

                ScrollView scrollView = findViewById(R.id.chatscroll);
                chatting.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT);
    }

    void callAPI(String sttText){
        messageList.add(new Message("입력 중. . .", Message.SENT_BY_BOT));

        JSONArray arr = new JSONArray();
        JSONObject baseAi = new JSONObject();
        JSONObject userMsg = new JSONObject();

        try {
            //AI 속성설정
            baseAi.put("role", "user");
            baseAi.put("content", "You are a helpful and kind talk AI Assistant.");
            //유저 메세지
            userMsg.put("role", "user");
            userMsg.put("content", sttText);
            //array로 담아서 한번에 보낸다
            arr.put(baseAi);
            arr.put(userMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject object = new JSONObject();
        try {
            //모델명 변경
            object.put("model", "gpt-3.5-turbo");
            object.put("messages", arr);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(object.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer "+MY_SECRET_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        //아래 result 받아오는 경로가 좀 수정되었다.
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    addResponse("Failed to load response due to " + response.body().string());
                }
            }
        });
    }

    //------인터페이스 구현 콜백처리-------//
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            //말하기 시작할 준비가 되면 호출
            Toast.makeText(getApplicationContext(),"음성인식 시작",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBeginningOfSpeech() {
            //말하기 시작했을 때 호출
        }

        @Override
        public void onRmsChanged(float v) {
            //입력받는 소리의 크기를 알려줌
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            //말을 시작하고 인식이 된 단어를 버퍼에 담음
        }

        @Override
        public void onEndOfSpeech() {
            //말하기를 중지하면 호출
        }

        @Override
        public void onError(int error) {
            //네트워크 또는 인식 오류가 발생했을 때 호출
            String message;

            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "클라이언트 에러";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "찾을 수 없음";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER 가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "말하는 시간초과";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }
            Toast.makeText(getApplicationContext(), "에러 발생 : " + message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle result) {
            //인식 결과가 준비되면 호출
            //말을 하면 ArrayList에 결과를 저장하고 textView에 단어를 이어줌

            ArrayList<String> matches =
                    result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            //matches: STT를 통해 인식된 문자열 결과물들이 저장된 ArrayList
            //for문을 이용해 ArrayList 결과를 가져와 textView에 단어 이어주는 과정 수행 후 출력
            for(int i =0;i<matches.size(); i++){
                ques.setText(matches.get(i));

                String sttResult = matches.get(0); // STT 결과 리스트의 첫번째 값만 사용
                addToChat(sttResult, Message.SENT_BY_ME);
                callAPI(sttResult);
            }
        }

        @Override
        public void onPartialResults(Bundle partresult) {
            //부분 인식 결과를 사용할 수 있을 때 호출
        }

        @Override
        public void onEvent(int i, Bundle params) {
            //향후 이벤트를 추가하기 위해 예약
        }
    };
}