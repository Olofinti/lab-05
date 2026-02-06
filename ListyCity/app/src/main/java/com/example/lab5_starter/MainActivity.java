package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private RecyclerView cityRecyclerView;

    private ArrayList<City> cityArrayList;
    private CityRecyclerAdapter cityRecyclerAdapter;

    // 1. Declare Firestore variables
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 2. Initialize Firestore
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityRecyclerView = findViewById(R.id.recycler_view_cities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityRecyclerAdapter = new CityRecyclerAdapter(this, cityArrayList, city -> {
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(), "City Details");
        });
        cityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cityRecyclerView.setAdapter(cityRecyclerAdapter);

        // 4. Add Snapshot Listener to sync with database
        citiesRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("Firestore", error.toString());
                    return;
                }
                if (value != null) {
                    cityArrayList.clear(); // Clear the old list
                    for (QueryDocumentSnapshot snapshot : value) {
                        // Get data from the document
                        String cityName = snapshot.getString("name");
                        String province = snapshot.getString("province");

                        if (cityName != null) { // defend against null values from firestore
                            cityArrayList.add(new City(cityName, province));
                        }
                    }
                    cityRecyclerAdapter.notifyDataSetChanged();
                }
            }
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(), "Add City");
        });

        // Swipe to delete functionality
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                City cityToDelete = cityArrayList.get(position);

                // Delete from Firestore using the city name as the ID
                citiesRef.document(cityToDelete.getName())
                        .delete()
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted successfully"))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error deleting city", e));
            }
        };
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(cityRecyclerView);
    }

    @Override
    public void updateCity(City city, String title, String year) {
        // if the name of the city is being changed, we need to delete the old
        // document and create a new one
        if (!city.getName().equals(title)) {
            citiesRef.document(city.getName()).delete();
        }
        // Update the city details in a map
        Map<String, Object> cityDetails = new HashMap<>();
        cityDetails.put("name", title);
        cityDetails.put("province", year);

        // set the new city information
        citiesRef.document(title).set(cityDetails)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "City updated successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating city", e));
    }

    @Override
    public void addCity(City city) {
        citiesRef.document(city.getName()).set(city);
    }
}
