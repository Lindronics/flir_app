package com.lindronics.flirapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.flir.thermalsdk.live.Identity;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CameraArrayAdapter extends ArrayAdapter<Identity> {
    private final Context context;
    private List<Identity> identityList;

    CameraArrayAdapter(Context context, List<Identity> identityList) {
        super(context, -1, identityList);
        this.context = context;
        this.identityList = identityList;
    }

    @NotNull
    @Override
    public View getView(int position, View rowView, @NotNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (rowView == null) {
            rowView = inflater.inflate(R.layout.camera_list_row, parent, false);
        }

        Identity cameraIdentity = identityList.get(position);

        TextView cameraName = rowView.findViewById(R.id.camera_name);
        cameraName.setText(cameraIdentity.deviceId);

        Button cameraActivityButton = rowView.findViewById(R.id.camera_activity_button);
        cameraActivityButton.setOnClickListener((View view) -> {

            // Convert identity to JSON, as it is not serializable and the class is final
            Gson gson = new Gson();
            String serializedIdentity = gson.toJson(cameraIdentity);

            Intent i = new Intent(context, CameraActivity.class);
            i.putExtra("cameraIdentity", serializedIdentity);
            context.startActivity(i);
        });

        return rowView;
    }

}
