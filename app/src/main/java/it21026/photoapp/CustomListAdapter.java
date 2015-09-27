package it21026.photoapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

public class CustomListAdapter extends BaseAdapter {

    private Activity activity;
    private LayoutInflater inflater;
    private List<PhotosLight> photoItems;
    ImageLoader imageLoader = VolleySingleton.getInstance().getImageLoader();

    public CustomListAdapter(Activity activity, List<PhotosLight> photoItems) {
        this.activity = activity;
        this.photoItems = photoItems;
    }

    @Override
    public int getCount() {
        return photoItems.size();
    }

    @Override
    public Object getItem(int position) {
        return photoItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null) inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = inflater.inflate(R.layout.photo_list_results_row, null);
        if (imageLoader == null) imageLoader = VolleySingleton.getInstance().getImageLoader();

        NetworkImageView thumbnail = (NetworkImageView) convertView.findViewById(R.id.PhotoListResult_Thumbnail);
        TextView name = (TextView)convertView.findViewById(R.id.PhotoListResult_Name);
        TextView description = (TextView)convertView.findViewById(R.id.PhotoListResult_Description);
        TextView date = (TextView)convertView.findViewById(R.id.PhotoListResult_Date);

        //Getting photo data for the item row
        PhotosLight p = photoItems.get(position);
        //Thumbnail image
        //Setting a default and error thumbnail in case of malfunction or network error
        imageLoader.get(p.getUrl(), ImageLoader.getImageListener(thumbnail, R.drawable.app_logo, R.drawable.photo_unavailable));
        thumbnail.setImageUrl(p.getUrl(), imageLoader);
        name.setText(p.getName());
        if(!p.getDescription().isEmpty()){
            description.setText(p.getDescription());
        }
        date.setText(p.getDate());

        return convertView;
    }
}
