package com.jessicathornsby.datalayer;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


public class SearchFragment extends Fragment implements   View.OnClickListener, View.OnFocusChangeListener, SearchView.OnQueryTextListener{


    protected Toolbar toolbar;
    private String id, name, phone, image_uri;
    private byte[] contactImage = null;
    private Bitmap bitmap;
    private int queryLength;
    private List<ContactItem> contactItems;
    private ListView listView;
    private ProgressBar progressBar;
    private ContactAdapter adapter;
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private int posicion;
    private ContactItem contactoElegido;
    private String jsonContacto;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);
        setHasOptionsMenu(true);

        contactItems = new ArrayList<>();
        toolbar = view.findViewById(R.id.tool_bar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        listView = view.findViewById(R.id.contact_list);
        progressBar = view.findViewById(R.id.progress_bar);

        new ContactInfo().execute();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                posicion = position;
                contactoElegido = contactItems.get(posicion);
                jsonContacto = new Gson().toJson(contactoElegido);
                final String nomContactoElegido = contactoElegido.getName();
                AlertDialog.Builder dialogo1 = new AlertDialog.Builder(getActivity());
                dialogo1.setMessage("¿ Confirma a " + nomContactoElegido + " como contacto de emergencia?");
                dialogo1.setCancelable(false);
                dialogo1.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogo1, int id) {
                        aceptar(nomContactoElegido);
                    }
                });
                dialogo1.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogo1, int id) {
                        cancelar();
                    }
                });
                dialogo1.show();
            }

            public void aceptar(String nombre) {
                Toast toast1 =
                        Toast.makeText(getActivity().getApplicationContext(),
                                nombre + " es ahora su contacto de emergencia", Toast.LENGTH_SHORT);
                toast1.show();

                try
                {
                    OutputStreamWriter fout=
                            new OutputStreamWriter(
                                    getActivity().openFileOutput("contacto_elegido.txt", Context.MODE_PRIVATE));

                    fout.write(jsonContacto);
                    Log.i("Ficheros", "Escribir fichero a memoria interna");
                    fout.close();
                }
                catch (Exception ex)
                {
                    Log.e("Ficheros", "Error al escribir fichero a memoria interna");
                }
            }

            public void cancelar() {
            }

        });

        return view;
    }




    private void readContacts() {

        ContentResolver cr = getActivity().getApplicationContext().getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                ContactItem item = new ContactItem();
                id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                image_uri = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        phone = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phone = phone.replaceAll("\\s+", "");
                        phone = phone.replaceAll("[^0-9]", "");
                    }
                    pCur.close();
                }
                if (image_uri != null) {
                    try {
                        bitmap = MediaStore.Images.Media
                                .getBitmap(getActivity().getApplicationContext().getContentResolver(),
                                        Uri.parse(image_uri));
                        contactImage = getImageBytes(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    contactImage = null;
                }
                item.setId(id);
                item.setName(name);
                item.setContactImage(contactImage);
                item.setPhone(phone);
                contactItems.add(item);
            }
        }
    }

    private byte[] getImageBytes(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return outputStream.toByteArray();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchMenuItem = menu.findItem(R.id.search);
        searchView.setQueryHint(getResources().getString(R.string.type_here));
        searchView.setOnQueryTextFocusChangeListener(this);
        searchView.setOnQueryTextListener(this);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            searchMenuItem.collapseActionView();
            searchView.setQuery("", false);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        queryLength = newText.length();
        adapter.getFilter().filter(newText);
        return false;
    }

    public class ContactInfo extends AsyncTask<Void, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            readContacts();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            setListAdapter(contactItems);
        }
    }

    private void setListAdapter(List<ContactItem> contactos) {
        adapter = new ContactAdapter(getActivity().getApplicationContext(), contactos);
        listView.setAdapter(adapter);
    }

}
