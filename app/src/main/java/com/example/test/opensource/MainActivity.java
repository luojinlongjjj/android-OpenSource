package com.example.test.opensource;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.test.opensource.bean.PeopleBean;

import yourpck.com.gson.Gson;
import yourpck.okhttp3.OkHttpClient;
import yourpck.okhttp3.internal.Version;
import yourpck.reactivex.Observable;
import yourpck.reactivex.ObservableEmitter;
import yourpck.reactivex.ObservableOnSubscribe;
import yourpck.reactivex.Observer;
import yourpck.retrofit2.Retrofit;
import yourpck.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import yourpck.retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = (TextView) findViewById(R.id.txt_info);
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        test();
    }

    public void test() {

        Gson gson = new Gson();

        PeopleBean peopleBean = new PeopleBean();
        peopleBean.setName("gson ok");

        String str = gson.toJson(peopleBean);
        printf(str);

        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                emitter.onNext(true);
                emitter.onComplete();
            }
        }).subscribeOn(yourpck.reactivex.schedulers.Schedulers.io()).observeOn(yourpck.reactivex.android.schedulers.AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(yourpck.reactivex.disposables.Disposable d) {

            }

            @Override
            public void onNext(Boolean aBoolean) {
                printf("rxjava ok");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });


        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        printf("okhttp3(" + Version.userAgent() + ") " + okHttpClient);


        Retrofit mRetrofit;

        mRetrofit = new Retrofit.Builder()
                .baseUrl("https://www.baidu.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build();
        printf("Retrofit " + mRetrofit);
    }

    void printf(String msg) {
        System.out.println(msg);
        info.append(msg);
        info.append("\n");
    }


}
