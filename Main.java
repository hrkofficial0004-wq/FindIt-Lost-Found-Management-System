import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * FindIt - Lost & Found Management System
 * Core Java + Swing + local text-file storage (no database).
 *
 * Run:
 *   javac -encoding UTF-8 -d out src/Main.java
 *   java -cp out Main
 *
 * Default admin login: admin / admin123
 */
public class Main {
    private static final String DATA_DIR = "data";
    private static final String USERS_FILE = DATA_DIR + "/users.txt";
    private static final String ITEMS_FILE = DATA_DIR + "/items.txt";
    private static final String CLAIMS_FILE = DATA_DIR + "/claims.txt";
    private static final String SEP = "\t";
    private static final Color NAVY = new Color(22, 34, 55);
    private static final Color BLUE = new Color(55, 112, 220);
    private static final Color BG = new Color(244, 247, 251);
    private static final Color TEXT = new Color(38, 49, 66);
    private static final Font NORMAL = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BOLD = new Font("Segoe UI", Font.BOLD, 14);

    private static String currentUser;
    private static boolean admin;
    private static JFrame frame;
    private static JPanel content;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            ensureStorage();
            showLogin();
        });
    }

    private static void ensureStorage() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            for (String file : Arrays.asList(USERS_FILE, ITEMS_FILE, CLAIMS_FILE)) {
                Path p = Paths.get(file);
                if (!Files.exists(p)) Files.createFile(p);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not create local data files: " + e.getMessage());
        }
    }

    private static java.util.List<String[]> readRows(String file, int columns) {
        java.util.List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] row = line.split(SEP, -1);
                if (row.length == columns) rows.add(row);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not read " + file + ": " + e.getMessage());
        }
        return rows;
    }

    private static void appendRow(String file, String[] values) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) line.append(SEP);
            line.append(clean(values[i]));
        }
        Files.write(Paths.get(file), (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replace("\t", " ").replace("\n", " ").replace("\r", " ").trim();
    }

    private static void rewriteRows(String file, java.util.List<String[]> rows) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) bw.write(SEP);
                    bw.write(clean(row[i]));
                }
                bw.newLine();
            }
        }
    }

    private static void showLogin() {
        JFrame login = new JFrame("FindIt | Login");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setSize(460, 480);
        login.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        JPanel top = new JPanel();
        top.setBackground(NAVY);
        top.setBorder(new EmptyBorder(28, 20, 28, 20));
        JLabel title = new JLabel("FindIt");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        JLabel sub = new JLabel("Lost something? Let's help you find it.");
        sub.setForeground(new Color(210, 220, 235));
        sub.setFont(NORMAL);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(title); top.add(Box.createVerticalStrut(8)); top.add(sub);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        form.setBorder(new EmptyBorder(24, 48, 24, 48));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(7, 0, 7, 0);
        JLabel userLabel = new JLabel("Username"); userLabel.setFont(BOLD);
        JTextField username = new JTextField(); styleField(username);
        JLabel passLabel = new JLabel("Password"); passLabel.setFont(BOLD);
        JPasswordField password = new JPasswordField(); styleField(password);
        JButton loginBtn = primaryButton("Login");
        JButton registerBtn = secondaryButton("Create student account");
        form.add(userLabel, g); g.gridy++; form.add(username, g);
        g.gridy++; form.add(passLabel, g); g.gridy++; form.add(password, g);
        g.gridy++; g.insets = new Insets(18, 0, 7, 0); form.add(loginBtn, g);
        g.gridy++; g.insets = new Insets(4, 0, 7, 0); form.add(registerBtn, g);

        JLabel hint = new JLabel("Admin demo: admin  |  Password: admin123", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12)); hint.setForeground(Color.GRAY);
        root.add(top, BorderLayout.NORTH); root.add(form, BorderLayout.CENTER); root.add(hint, BorderLayout.SOUTH);
        login.setContentPane(root);
        loginBtn.addActionListener(e -> {
            String u = username.getText().trim();
            String p = new String(password.getPassword());
            if ("admin".equals(u) && "admin123".equals(p)) {
                currentUser = "admin"; admin = true; login.dispose(); showMain();
            } else if (validUser(u, p)) {
                currentUser = u; admin = false; login.dispose(); showMain();
            } else {
                JOptionPane.showMessageDialog(login, "Invalid username or password.", "Login failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        registerBtn.addActionListener(e -> registerDialog(login));
        login.getRootPane().setDefaultButton(loginBtn);
        login.setVisible(true);
    }

    private static boolean validUser(String username, String password) {
        for (String[] u : readRows(USERS_FILE, 3))
            if (u[0].equalsIgnoreCase(username) && u[1].equals(password)) return true;
        return false;
    }

    private static void registerDialog(Component parent) {
        JTextField name = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Full name")); panel.add(name);
        panel.add(new JLabel("Username")); panel.add(username);
        panel.add(new JLabel("Password")); panel.add(password);
        int result = JOptionPane.showConfirmDialog(parent, panel, "Student registration", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        String n = name.getText().trim(), u = username.getText().trim(), p = new String(password.getPassword());
        if (n.isEmpty() || u.isEmpty() || p.length() < 4) {
            JOptionPane.showMessageDialog(parent, "Enter all details. Password must contain at least 4 characters."); return;
        }
        if (u.equalsIgnoreCase("admin")) {
            JOptionPane.showMessageDialog(parent, "That username is reserved."); return;
        }
        for (String[] existing : readRows(USERS_FILE, 3)) {
            if (existing[0].equalsIgnoreCase(u)) {
                JOptionPane.showMessageDialog(parent, "Username already exists."); return;
            }
        }
        try {
            appendRow(USERS_FILE, new String[]{u, p, n});
            JOptionPane.showMessageDialog(parent, "Account created. You can now log in.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not save account: " + ex.getMessage());
        }
    }

    private static void showMain() {
        frame = new JFrame("FindIt | Lost & Found Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1000, 650));
        frame.setSize(1180, 760);
        frame.setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(NAVY);
        sidebar.setBorder(new EmptyBorder(24, 14, 20, 14));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("FindIt");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 27)); brand.setForeground(Color.WHITE);
        JLabel role = new JLabel(admin ? "ADMIN PANEL" : "STUDENT PANEL");
        role.setForeground(new Color(170, 190, 220)); role.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sidebar.add(brand); sidebar.add(Box.createVerticalStrut(5)); sidebar.add(role); sidebar.add(Box.createVerticalStrut(30));
        addNav(sidebar, "Dashboard", () -> showDashboard());
        addNav(sidebar, "Report Lost Item", () -> showItemForm("LOST"));
        addNav(sidebar, "Report Found Item", () -> showItemForm("FOUND"));
        addNav(sidebar, "Browse Items", () -> showBrowse());
        addNav(sidebar, admin ? "Manage Claims" : "My Claims", () -> showClaims());
        sidebar.add(Box.createVerticalGlue());
        JLabel signed = new JLabel("Signed in: " + currentUser);
        signed.setForeground(new Color(190, 205, 225)); signed.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sidebar.add(signed); sidebar.add(Box.createVerticalStrut(12));
        JButton logout = new JButton("Log out"); logout.setFocusPainted(false);
        logout.addActionListener(e -> { frame.dispose(); currentUser = null; admin = false; showLogin(); });
        sidebar.add(logout);

        content = new JPanel(new BorderLayout());
        content.setBackground(BG);
        root.add(sidebar, BorderLayout.WEST); root.add(content, BorderLayout.CENTER);
        frame.setContentPane(root);
        showDashboard();
        frame.setVisible(true);
    }

    private static void addNav(JPanel side, String label, Runnable action) {
        JButton b = new JButton(label);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setForeground(Color.WHITE); b.setBackground(new Color(34, 51, 78));
        b.setBorder(new EmptyBorder(10, 13, 10, 8)); b.setFocusPainted(false);
        b.setFont(NORMAL); b.addActionListener(e -> action.run());
        side.add(b); side.add(Box.createVerticalStrut(7));
    }

    private static void page(String title, String subtitle) {
        content.removeAll();
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG); page.setBorder(new EmptyBorder(28, 30, 28, 30));
        JPanel heading = new JPanel(); heading.setBackground(BG); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); t.setFont(new Font("Segoe UI", Font.BOLD, 26)); t.setForeground(TEXT);
        JLabel s = new JLabel(subtitle); s.setFont(NORMAL); s.setForeground(new Color(110, 122, 140));
        heading.add(t); heading.add(Box.createVerticalStrut(5)); heading.add(s);
        page.add(heading, BorderLayout.NORTH);
        content.add(page, BorderLayout.CENTER);
        content.putClientProperty("page", page);
    }

    private static JPanel currentPage() { return (JPanel) content.getClientProperty("page"); }

    private static void showDashboard() {
        page("Dashboard", "A simple place to report, search and recover misplaced belongings.");
        JPanel p = currentPage();
        java.util.List<String[]> items = readRows(ITEMS_FILE, 10);
        int lost = 0, found = 0, returned = 0;
        for (String[] it : items) {
            if (it[1].equals("LOST")) lost++;
            if (it[1].equals("FOUND")) found++;
            if (it[8].equals("RETURNED")) returned++;
        }
        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 14)); stats.setOpaque(false);
        stats.add(statCard("Lost reports", String.valueOf(lost), "Items reported missing"));
        stats.add(statCard("Found reports", String.valueOf(found), "Items waiting to be claimed"));
        stats.add(statCard("Returned items", String.valueOf(returned), "Successfully reunited"));
        JPanel center = new JPanel(new BorderLayout(0, 18)); center.setOpaque(false);
        center.add(stats, BorderLayout.NORTH);
        JPanel welcome = new JPanel(); welcome.setBackground(Color.WHITE); welcome.setBorder(new EmptyBorder(24, 24, 24, 24));
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        JLabel w = new JLabel("Welcome, " + (admin ? "Administrator" : currentUser) + "!");
        w.setFont(new Font("Segoe UI", Font.BOLD, 20)); w.setForeground(TEXT);
        JLabel desc = new JLabel("<html>Use the menu to report a lost or found item, browse reports, or follow up on claims.</html>");
        desc.setFont(NORMAL); desc.setForeground(new Color(90, 102, 120));
        welcome.add(w); welcome.add(Box.createVerticalStrut(10)); welcome.add(desc);
        center.add(welcome, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        content.revalidate(); content.repaint();
    }

    private static JPanel statCard(String label, String value, String detail) {
        JPanel card = new JPanel(); card.setBackground(Color.WHITE); card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label); l.setForeground(new Color(100, 112, 130)); l.setFont(BOLD);
        JLabel v = new JLabel(value); v.setForeground(BLUE); v.setFont(new Font("Segoe UI", Font.BOLD, 30));
        JLabel d = new JLabel(detail); d.setForeground(Color.GRAY); d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        card.add(l); card.add(Box.createVerticalStrut(9)); card.add(v); card.add(Box.createVerticalStrut(5)); card.add(d);
        return card;
    }

    private static void showItemForm(String type) {
        page(type.equals("LOST") ? "Report a Lost Item" : "Report a Found Item",
                "Provide clear details so other students can identify the item.");
        JPanel p = currentPage();
        JPanel form = new JPanel(new GridBagLayout()); form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(22, 24, 22, 24));
        GridBagConstraints g = new GridBagConstraints(); g.gridx = 0; g.gridy = 0; g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(7, 7, 7, 7);
        JTextField name = new JTextField(), location = new JTextField(), date = new JTextField();
        JTextField contact = new JTextField();
        JTextArea description = new JTextArea(4, 25); description.setLineWrap(true); description.setWrapStyleWord(true);
        JComboBox<String> category = new JComboBox<>(new String[]{"Electronics", "ID Card", "Books/Notes", "Bag", "Keys", "Wallet", "Clothing", "Other"});
        styleField(name); styleField(location); styleField(date); styleField(contact);
        addFormRow(form, g, "Item name", name);
        addFormRow(form, g, "Category", category);
        addFormRow(form, g, "Location (where lost/found)", location);
        addFormRow(form, g, "Date (e.g. 10-Nov-2026)", date);
        addFormRow(form, g, "Contact information", contact);
        JScrollPane ds = new JScrollPane(description); ds.setPreferredSize(new Dimension(300, 90));
        addFormRow(form, g, "Description", ds);
        JButton save = primaryButton("Submit report");
        g.gridy++; g.gridx = 1; g.insets = new Insets(15, 7, 7, 7); form.add(save, g);
        save.addActionListener(e -> {
            if (name.getText().trim().isEmpty() || location.getText().trim().isEmpty()
                    || date.getText().trim().isEmpty() || contact.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please complete item name, location, date and contact."); return;
            }
            String id = "FI" + System.currentTimeMillis();
            String[] row = {id, type, name.getText(), (String) category.getSelectedItem(), location.getText(),
                    date.getText(), description.getText(), contact.getText(), "OPEN", currentUser};
            try {
                appendRow(ITEMS_FILE, row);
                JOptionPane.showMessageDialog(frame, "Report submitted successfully.\nYour report ID: " + id);
                showBrowse();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Could not save report: " + ex.getMessage());
            }
        });
        p.add(new JScrollPane(form), BorderLayout.CENTER);
        content.revalidate(); content.repaint();
    }

    private static void addFormRow(JPanel panel, GridBagConstraints g, String label, JComponent field) {
        g.gridx = 0; g.weightx = 0.30; g.gridy++;
        JLabel l = new JLabel(label); l.setFont(BOLD); panel.add(l, g);
        g.gridx = 1; g.weightx = 0.70; panel.add(field, g);
    }

    private static void showBrowse() {
        page("Browse Items", "Search all open lost and found reports.");
        JPanel p = currentPage();
        JPanel top = new JPanel(new BorderLayout(8, 8)); top.setOpaque(false);
        JTextField search = new JTextField(); styleField(search);
        JComboBox<String> filter = new JComboBox<>(new String[]{"ALL", "LOST", "FOUND"});
        JButton find = primaryButton("Search");
        JPanel controls = new JPanel(new BorderLayout(8, 8)); controls.setOpaque(false);
        controls.add(search, BorderLayout.CENTER); controls.add(filter, BorderLayout.WEST); controls.add(find, BorderLayout.EAST);
        top.add(controls, BorderLayout.CENTER);
        String[] cols = {"Report ID", "Type", "Item", "Category", "Location", "Date", "Description", "Contact", "Status", "Reported by"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(model); table.setRowHeight(28); table.setFont(NORMAL); table.getTableHeader().setFont(BOLD);
        JScrollPane scroll = new JScrollPane(table);
        JButton claim = primaryButton("Request to claim selected item");
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT)); bottom.setOpaque(false);
        bottom.add(claim);
        Runnable load = () -> {
            model.setRowCount(0);
            String q = search.getText().trim().toLowerCase();
            String f = (String) filter.getSelectedItem();
            for (String[] it : readRows(ITEMS_FILE, 10)) {
                if (!it[8].equals("OPEN")) continue;
                if (!f.equals("ALL") && !it[1].equals(f)) continue;
                String searchable = String.join(" ", it).toLowerCase();
                if (!q.isEmpty() && !searchable.contains(q)) continue;
                model.addRow(it);
            }
        };
        find.addActionListener(e -> load.run());
        claim.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected < 0) { JOptionPane.showMessageDialog(frame, "Select an item first."); return; }
            String id = String.valueOf(model.getValueAt(selected, 0));
            String owner = String.valueOf(model.getValueAt(selected, 9));
            if (!admin && owner.equals(currentUser)) { JOptionPane.showMessageDialog(frame, "You cannot claim your own report."); return; }
            String message = JOptionPane.showInputDialog(frame, "Add a short message for the reporter:");
            if (message == null || message.trim().isEmpty()) return;
            try {
                appendRow(CLAIMS_FILE, new String[]{"CL" + System.currentTimeMillis(), id, currentUser, message, "PENDING"});
                JOptionPane.showMessageDialog(frame, "Claim request sent. The reporter/admin can review it.");
            } catch (IOException ex) { JOptionPane.showMessageDialog(frame, "Could not save claim: " + ex.getMessage()); }
        });
        load.run();
        p.add(top, BorderLayout.NORTH); p.add(scroll, BorderLayout.CENTER); p.add(bottom, BorderLayout.SOUTH);
        content.revalidate(); content.repaint();
    }

    private static void showClaims() {
        page(admin ? "Manage Claims" : "My Claims", admin ? "Review claim requests and update their status." : "Track claim requests you have submitted.");
        JPanel p = currentPage();
        String[] cols = {"Claim ID", "Item ID", "Requested by", "Message", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable table = new JTable(model); table.setRowHeight(28); table.setFont(NORMAL); table.getTableHeader().setFont(BOLD);
        Runnable load = () -> {
            model.setRowCount(0);
            for (String[] c : readRows(CLAIMS_FILE, 5))
                if (admin || c[2].equals(currentUser)) model.addRow(c);
        };
        load.run();
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT)); bottom.setOpaque(false);
        if (admin) {
            JButton approve = primaryButton("Approve selected");
            JButton reject = secondaryButton("Reject selected");
            JButton returned = secondaryButton("Mark item returned");
            bottom.add(approve); bottom.add(reject); bottom.add(returned);
            approve.addActionListener(e -> updateClaim(table, model, "APPROVED", load));
            reject.addActionListener(e -> updateClaim(table, model, "REJECTED", load));
            returned.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r < 0) { JOptionPane.showMessageDialog(frame, "Select a claim first."); return; }
                String itemId = String.valueOf(model.getValueAt(r, 1));
                java.util.List<String[]> items = readRows(ITEMS_FILE, 10);
                for (String[] it : items) if (it[0].equals(itemId)) it[8] = "RETURNED";
                try { rewriteRows(ITEMS_FILE, items); JOptionPane.showMessageDialog(frame, "Item marked as returned."); }
                catch (IOException ex) { JOptionPane.showMessageDialog(frame, ex.getMessage()); }
            });
        }
        p.add(new JScrollPane(table), BorderLayout.CENTER); p.add(bottom, BorderLayout.SOUTH);
        content.revalidate(); content.repaint();
    }

    private static void updateClaim(JTable table, DefaultTableModel model, String status, Runnable reload) {
        int r = table.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(frame, "Select a claim first."); return; }
        String id = String.valueOf(model.getValueAt(r, 0));
        java.util.List<String[]> claims = readRows(CLAIMS_FILE, 5);
        for (String[] c : claims) if (c[0].equals(id)) c[4] = status;
        try { rewriteRows(CLAIMS_FILE, claims); reload.run(); }
        catch (IOException ex) { JOptionPane.showMessageDialog(frame, "Could not update claim: " + ex.getMessage()); }
    }

    private static void styleField(JTextField field) {
        field.setFont(NORMAL); field.setPreferredSize(new Dimension(220, 34));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(215, 222, 232)),
                new EmptyBorder(5, 8, 5, 8)));
    }

    private static JButton primaryButton(String text) {
        JButton b = new JButton(text); b.setFont(BOLD); b.setForeground(Color.WHITE); b.setBackground(BLUE);
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(10, 16, 10, 16)); return b;
    }
    private static JButton secondaryButton(String text) {
        JButton b = new JButton(text); b.setFont(BOLD); b.setForeground(TEXT); b.setBackground(new Color(230, 235, 243));
        b.setFocusPainted(false); b.setBorder(new EmptyBorder(10, 16, 10, 16)); return b;
    }
}
