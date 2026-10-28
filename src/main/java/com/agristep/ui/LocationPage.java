package com.agristep.ui;

import com.agristep.AppContext;
import com.agristep.domain.GardenSettings;
import com.agristep.weather.GeoPlace;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import java.util.Locale;

/**
 * Where the garden is. The person can type the place and coordinates themselves, search for a town,
 * or pick one from a list of common Philippine places (the list works without internet).
 */
final class LocationPage extends Page {
    record Spot(String name, double latitude, double longitude) {}

    static final List<Spot> COMMON_PLACES = List.of(
            new Spot("Manila", 14.5995, 120.9842),
            new Spot("Quezon City", 14.6760, 121.0437),
            new Spot("Baguio", 16.4023, 120.5960),
            new Spot("Laoag", 18.1978, 120.5936),
            new Spot("Tuguegarao", 17.6132, 121.7270),
            new Spot("Angeles", 15.1450, 120.5887),
            new Spot("Tagaytay", 14.1153, 120.9621),
            new Spot("Batangas City", 13.7565, 121.0583),
            new Spot("Lucena", 13.9373, 121.6172),
            new Spot("Naga", 13.6218, 123.1948),
            new Spot("Legazpi", 13.1391, 123.7438),
            new Spot("Puerto Princesa", 9.7392, 118.7353),
            new Spot("Cebu City", 10.3157, 123.8854),
            new Spot("Iloilo City", 10.7202, 122.5621),
            new Spot("Bacolod", 10.6767, 122.9500),
            new Spot("Tacloban", 11.2543, 124.9630),
            new Spot("Dumaguete", 9.3068, 123.3054),
            new Spot("Tagbilaran", 9.6500, 123.8500),
            new Spot("Cagayan de Oro", 8.4542, 124.6319),
            new Spot("Iligan", 8.2280, 124.2452),
            new Spot("Butuan", 8.9475, 125.5406),
            new Spot("Davao City", 7.1907, 125.4553),
            new Spot("Kidapawan", 7.0083, 125.0894),
            new Spot("General Santos", 6.1164, 125.1716),
            new Spot("Cotabato City", 7.2236, 124.2464),
            new Spot("Zamboanga City", 6.9214, 122.0790));

    private final SearchField searchQuery = new SearchField("Type a town or city");
    private final JPanel searchResults = new JPanel(new StackLayout(8));
    private final WrapLabel searchStatus = WrapLabel.muted("");

    private final SearchField placeName = new SearchField("Your barangay, town or city");
    private final SearchField latitude = new SearchField("-90 to 90, e.g. 14.5995");
    private final SearchField longitude = new SearchField("-180 to 180, e.g. 120.9842");
    private final WrapLabel formError = new WrapLabel("", Theme.BODY_BOLD, Theme.SILI);

    private final SearchField filter = new SearchField("Filter the list, e.g. Cebu");
    private final JPanel commonGrid = new JPanel(new FlowGridLayout(140, 4, 8, 0));
    private int generation;

    LocationPage(AppContext ctx, Navigator nav) {
        super(ctx, nav);
        searchResults.setOpaque(false);
        commonGrid.setOpaque(false);
        formError.setVisible(false);
        searchQuery.addActionListener(e -> search());
        for (SearchField f : new SearchField[] {placeName, latitude, longitude}) f.addActionListener(e -> saveTyped());
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fillCommon(); }
            @Override public void removeUpdate(DocumentEvent e) { fillCommon(); }
            @Override public void changedUpdate(DocumentEvent e) { fillCommon(); }
        });
    }

    @Override void onShow() {
        searchResults.removeAll();
        searchStatus.setText("");
        showFormError("");
        Tasks.run(ctx.settings::current, this::render, this::fail);
    }

    private void render(GardenSettings current) {
        placeName.setText(current.getLocationName());
        latitude.setText(String.format(Locale.ROOT, "%.4f", current.getLatitude()));
        longitude.setText(String.format(Locale.ROOT, "%.4f", current.getLongitude()));

        Card header = new Card().gap(6);
        header.add(WrapLabel.h1("Location"));
        header.add(WrapLabel.muted("Your garden is at " + current.getLocationName() + " ("
                + String.format(Locale.ROOT, "%.4f, %.4f", current.getLatitude(), current.getLongitude())
                + "). The weather and the crop ranking use this place."));

        Card edit = new Card().gap(10);
        edit.add(WrapLabel.h3("Where is your garden?"));
        edit.add(WrapLabel.muted("Type the name of your place and its coordinates, then save. "
                + "Not sure of the coordinates? Search or pick a place below and they are filled in for you."));
        edit.add(labelled("Place name", placeName));
        JPanel coords = new JPanel(new FlowGridLayout(150, 2, 8, 0));
        coords.setOpaque(false);
        coords.add(labelled("Latitude", latitude));
        coords.add(labelled("Longitude", longitude));
        edit.add(coords);
        edit.add(formError);
        PillButton save = PillButton.primary("Save location");
        save.addActionListener(e -> saveTyped());
        PillButton undo = PillButton.secondary("Reset");
        undo.addActionListener(e -> {
            placeName.setText(current.getLocationName());
            latitude.setText(String.format(Locale.ROOT, "%.4f", current.getLatitude()));
            longitude.setText(String.format(Locale.ROOT, "%.4f", current.getLongitude()));
            showFormError("");
        });
        JPanel buttons = new JPanel(new FlowGridLayout(120, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(undo);
        edit.add(buttons);

        Card searchCard = new Card().gap(10);
        searchCard.add(WrapLabel.h3("Search for your town"));
        JPanel row = new JPanel(new RowLayout(8, false));
        row.setOpaque(false);
        PillButton go = PillButton.primary("Search");
        go.addActionListener(e -> search());
        row.add(searchQuery, RowLayout.CENTER);
        row.add(go, RowLayout.EAST);
        searchCard.add(row);
        searchCard.add(searchStatus);
        searchCard.add(searchResults);

        Card common = new Card().gap(10);
        common.add(WrapLabel.h3("Pick a common place"));
        common.add(WrapLabel.muted("These work even without internet."));
        common.add(filter);
        common.add(commonGrid);
        fillCommon();

        setContent(header, edit, searchCard, common);
    }

    private static JPanel labelled(String label, java.awt.Component input) {
        JPanel box = new JPanel(new StackLayout(4));
        box.setOpaque(false);
        box.add(new WrapLabel(label, Theme.SMALL_BOLD, Theme.MUTED));
        box.add(input);
        return box;
    }

    // ---- typed location ----

    private void showFormError(String text) {
        formError.setText(text);
        formError.setVisible(!text.isEmpty());
        revalidate();
        repaint();
    }

    /** Reads a coordinate typed by a person; accepts a comma as the decimal mark. */
    static double parseCoordinate(String text, String what, double limit) {
        String cleaned = text == null ? "" : text.trim().replace(',', '.');
        double value;
        try {
            value = Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a number between -" + (int) limit + " and " + (int) limit + ".");
        }
        if (!Double.isFinite(value) || Math.abs(value) > limit) {
            throw new IllegalArgumentException(what + " must be a number between -" + (int) limit + " and " + (int) limit + ".");
        }
        return value;
    }

    private void saveTyped() {
        double lat;
        double lon;
        String name = placeName.getText().trim();
        try {
            if (name.isEmpty()) throw new IllegalArgumentException("Type the name of your place.");
            lat = parseCoordinate(latitude.getText(), "Latitude", 90);
            lon = parseCoordinate(longitude.getText(), "Longitude", 180);
        } catch (IllegalArgumentException e) {
            showFormError(e.getMessage());
            return;
        }
        showFormError("");
        choose(name, lat, lon);
    }

    // ---- search ----

    private void search() {
        String text = searchQuery.getText().trim();
        if (text.length() < 2) {
            searchStatus.setText("Type at least 2 letters.");
            return;
        }
        int mine = ++generation;
        searchStatus.setText("Searching…");
        searchResults.removeAll();
        Tasks.run(() -> ctx.weather.searchPlaces(text), places -> {
            if (mine != generation) return;
            show(places, text);
        }, t -> {
            if (mine != generation) return;
            searchStatus.setText("Search isn't available right now (" + Tasks.messageOf(t)
                    + "). Type your place above or pick a common place below instead.");
        });
    }

    private void show(List<GeoPlace> places, String text) {
        searchResults.removeAll();
        searchStatus.setText(places.isEmpty() ? "No place found for \"" + text + "\". Try another spelling." : "");
        for (GeoPlace place : places) {
            Card card = new Card(Theme.WHITE).gap(2).padding(10);
            card.add(WrapLabel.bold(place.label()));
            card.add(WrapLabel.small(String.format(Locale.ROOT, "%.2f, %.2f", place.latitude(), place.longitude())));
            card.onClick(() -> choose(place.label(), place.latitude(), place.longitude()));
            searchResults.add(card);
        }
        searchResults.revalidate();
        searchResults.repaint();
        revalidate();
    }

    // ---- common places ----

    private void fillCommon() {
        String q = filter.getText().trim().toLowerCase(Locale.ROOT);
        commonGrid.removeAll();
        int shown = 0;
        for (Spot spot : COMMON_PLACES) {
            if (!q.isEmpty() && !spot.name().toLowerCase(Locale.ROOT).contains(q)) continue;
            PillButton b = PillButton.secondary(spot.name());
            b.addActionListener(e -> choose(spot.name(), spot.latitude(), spot.longitude()));
            commonGrid.add(b);
            shown++;
        }
        if (shown == 0) commonGrid.add(WrapLabel.muted("No common place matches. Use the form above to type yours."));
        commonGrid.revalidate();
        commonGrid.repaint();
        revalidate();
    }

    private void choose(String name, double lat, double lon) {
        Tasks.run(() -> ctx.settings.changeLocation(name, lat, lon), saved -> {
            nav.flash("Location set to " + name + ".", false);
            nav.show(Navigator.TODAY);
        }, t -> showFormError(Tasks.messageOf(t)));
    }
}
