package com.lonek.sort_listview;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;


import com.lonek.test.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private Context mContext;
    private Activity mActivity;

    private RefreshListView sortListView;
    SlideLinerLayout sideBar;
    private TextView dialog;
    private SortAdapter adapter;
    private List<String> mOriginDataList;
    /**
     * 汉字转换成拼音的类
     */
    private CharacterParser characterParser;
    private List<SortModel> SourceDateList;
    private List<SortModel> newSorceDatalist;

    /**
     * 根据拼音来排列ListView里面的数据类
     */
    private PinyinComparator pinyinComparator;

    private static final int MSG_HIDE_HEADER = 111;
    private static final int MSG_HIDE_FOOTER = 112;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_HIDE_FOOTER:
                    sortListView.hideFootView();
                    SourceDateList.clear();
                    SourceDateList.addAll(filledData(mOriginDataList));
                    Collections.sort(SourceDateList, pinyinComparator);
                    adapter.notifyDataSetChanged();
                    break;
                case MSG_HIDE_HEADER:
                    sortListView.hideHeadView();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this.getApplicationContext();
        mActivity = MainActivity.this;

        initProperty();

        initViews();
    }

    private void initProperty() {
        characterParser = CharacterParser.getInstance();
        pinyinComparator = new PinyinComparator();
    }

    private void initViews() {
        sortListView = (RefreshListView) findViewById(R.id.lv_sort);
        sideBar = (SlideLinerLayout) findViewById(R.id.sidrbar);
        dialog = (TextView) findViewById(R.id.dialog);
        sideBar.setTextDialog(dialog);

        //设置右侧触摸监听
        sideBar.setOnTouchingLetterChangedListener(new SlideLinerLayout.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                //该字母首次出现的位置
                int position = adapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    stopScroll(sortListView);
                    sortListView.setSelection(position);
                }else{

                }

            }
        });

        sortListView.setUserClassSimpleName(this.getClass().getSimpleName());
        sortListView.setRefreshListener(new RefreshListView.OnRefreshListener() {
            @Override
            public void onPullRefresh() {
                mOriginDataList.add("叶良辰");
                mOriginDataList.add("赵日天");
                mOriginDataList.add("王思聪");
                Message message = Message.obtain();
                message.what = MSG_HIDE_HEADER;
                mHandler.sendMessageDelayed(message, 3000);
            }

            @Override
            public void onPullLoadMore() {
                mOriginDataList.add("L叶良辰");
                mOriginDataList.add("s赵日天");
                mOriginDataList.add("D王思聪");
                Message message = Message.obtain();
                message.what = MSG_HIDE_FOOTER;
                mHandler.sendMessageDelayed(message, 3000);

            }
        });

        sortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //这里要利用adapter.getItem(position)来获取当前position所对应的对象
                Toast.makeText(getApplication(), ((SortModel) adapter.getItem(position - 1)).getName(), Toast.LENGTH_SHORT).show();
            }
        });
        mOriginDataList = Arrays.asList(getResources().getStringArray(R.array.date));
        mOriginDataList = new ArrayList<>(mOriginDataList);
        SourceDateList = filledData(Arrays.asList(getResources().getStringArray(R.array.date)));
        // 根据a-z进行排序源数据
        Collections.sort(SourceDateList, pinyinComparator);
        adapter = new SortAdapter(this, SourceDateList);
        sortListView.setAdapter(adapter);
        sortListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL){
                    dialog.setVisibility(View.VISIBLE);
                }else{
                    dialog.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                String letter = ((SortModel)adapter.getItem(firstVisibleItem)).getSortLetters();
                if(letter != null && !letter.equals("") && sideBar.mListViewScrollAble){
                    sideBar.setChoosed(letter);
                    dialog.setText(letter);
                }
            }
        });
    }

    /**
     * 为ListView填充数i据
     *
     * @param data
     * @return
     */
    private List<SortModel> filledData(List<String> data) {
        List<SortModel> mSortList = new ArrayList<SortModel>();

        for (int i = 0; i < data.size(); i++) {
            SortModel sortModel = new SortModel();
            sortModel.setName(data.get(i));
            //汉字转换成拼音
            String pinyin = characterParser.getSelling(data.get(i));
            String sortString = pinyin.substring(0, 1).toUpperCase();

            // 正则表达式，判断首字母是否是英文字母
            if (sortString.matches("[A-Z]")) {
                sortModel.setSortLetters(sortString.toUpperCase());
            } else {
                sortModel.setSortLetters("#");
            }

            mSortList.add(sortModel);
        }
        return mSortList;
    }

    /**
     * 反射使得listView停止滚动
     * @param view
     */
    private void stopScroll(AbsListView view)
    {
        try
        {
            Field field = android.widget.AbsListView.class.getDeclaredField("mFlingRunnable");
            field.setAccessible(true);
            Object flingRunnable = field.get(view);
            if (flingRunnable != null)
            {
                Method method = Class.forName("android.widget.AbsListView$FlingRunnable").getDeclaredMethod("endFling");
                method.setAccessible(true);
                method.invoke(flingRunnable);
            }
        }
        catch (Exception e) {}
    }


}
