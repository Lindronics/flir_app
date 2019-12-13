package com.lindronics.flirapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.flir.thermalsdk.live.Identity;

import java.util.HashMap;
import java.util.List;

public class CameraArrayAdapter extends ArrayAdapter<Identity> {
    private final Context context;
    private List<Identity> identityList;

    public CameraArrayAdapter(Context context, List<Identity> identityList) {
        super(context, -1, identityList);
        this.context = context;
        this.identityList = identityList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.camera_list_row, parent, false);

        TextView cameraName = rowView.findViewById(R.id.camera_name);
        Identity cameraIdentity = identityList.get(position);
        cameraName.setText(cameraIdentity.deviceId);

        return rowView;
    }

}
