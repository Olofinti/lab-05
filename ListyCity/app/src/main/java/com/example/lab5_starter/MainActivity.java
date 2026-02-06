package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // 1. Declare Firestore variables [cite: 187, 198]
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 2. Initialize Firestore [cite: 191, 203]
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
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // 3. REMOVED addDummyData(); as per lab instructions to remove hardcoded data [cite: 183]

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
                        // Get data from the document [cite: 215, 216]
                        String name = snapshot.getId(); // Using ID as name usually, or snapshot.getString("name");
                        // Ideally strictly follow slide 15:
                        String cityName = snapshot.getString("name");
                        String province = snapshot.getString("province");

                        cityArrayList.add(new City(cityName, province));
                    }
                    cityArrayAdapter.notifyDataSetChanged();
                }
            }
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        // Existing click listener for EDITING
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

        // 5. NEW: Long Click Listener for DELETING (Participation Exercise)
        cityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Get the city to delete
                City cityToDelete = cityArrayList.get(position);

                // Delete from Firestore using the city name as the ID
                citiesRef.document(cityToDelete.getCityName())
                        .delete()
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "City deleted successfully"))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error deleting city", e));

                return true; // Return true to indicate the click was handled
            }
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        // Optional: You can implement update logic here by overwriting the document
        // This is not strictly required for the specific "Delete" exercise but is good practice.
    }

    @Override
    public void addCity(City city){
        // 6. Update addCity to write to Firestore
        // We do NOT need to manually add to cityArrayList here because the
        // SnapshotListener (step 4) will automatically detect the change and update the list.

        citiesRef.document(city.getCityName()).set(city);
    }
}