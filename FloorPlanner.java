import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;


public class FloorPlanner extends JFrame {
    public static final int GRID_SIZE = 20;
    public static final int drag = 2;
    public static final int CANVAS_WIDTH = 800;
    public static final int CANVAS_HEIGHT = 600;
    
    public JPanel controlPanel;
    public DrawingCanvas canvas;
    public ArrayList<Room> rooms;
    public Room selectedRoom;
    public Room draggedRoom;
    public Point dragStart;
    public Room referenceRoom;
    public JComboBox<String> roomTypeCombo;
    public JTextField widthField, heightField;
    public JComboBox<String> directionCombo;
    public JComboBox<String> alignmentCombo;
    public boolean isAddingDoor = false;
    public boolean isAddingWindow = false;
    public boolean isVerticalWindow = false;
    public boolean isVerticalDoor = false;
    public JPanel furniturePanel;
    public JPanel fixturesPanel;
    public Map<String, ImageIcon> furnitureIcons;
    public Map<String, ImageIcon> fixtureIcons;
    public String selectedFurniture = null;
    public String selectedFixture = null;
    public Point draggedItemStart = null;
    public ArrayList<FurnitureItem> furnitureItems = new ArrayList<>();

    // Room Colors
    public static final Color BEDROOM_COLOR = new Color(144, 238, 144);
    public static final Color BATHROOM_COLOR = new Color(135, 206, 235);
    public static final Color KITCHEN_COLOR = new Color(210, 4, 45);
    public static final Color LIVING_COLOR = new Color(255, 255, 0);

    public static final Map<String, Dimension> FURNITURE_DIMENSIONS = new HashMap<>() {{
        put("bed", new Dimension(30, 40));
        put("chair", new Dimension(20, 20));
        put("table", new Dimension(44, 30));
        put("sofa", new Dimension(52, 32));
        put("dining_set", new Dimension(46, 23));
    }};

    public static final Map<String, Dimension> FIXTURE_DIMENSIONS = new HashMap<>() {{
        put("commode", new Dimension(40, 50));
        put("washbasin", new Dimension(50, 40));
        put("shower", new Dimension(35, 40));
        put("kitchen_sink", new Dimension(45, 40));
        put("stove", new Dimension(40, 35));
    }};
    
    public FloorPlanner() {
        setTitle("2D Floor Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        rooms = new ArrayList<>();
        loadIcons();
        initializeComponents();
        layoutComponents();
        
        setVisible(true);
    }

    public void loadIcons() {
        furnitureIcons = new HashMap<>();
        fixtureIcons = new HashMap<>();
        
        // Load furniture icons
        String[] furnitureTypes = {"bed", "chair", "table", "sofa", "dining_set"};
        for (String type : furnitureTypes) {
            try {
                BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/furniture/" + type + ".png"));
                // Use better quality image scaling
                BufferedImage scaledImg = new BufferedImage(40, 10, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = scaledImg.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(img, 0, 0, 40, 10, null);
                g2d.dispose();
                furnitureIcons.put(type, new ImageIcon(scaledImg));
            } catch (IOException e) {
                System.err.println("Could not load furniture icon: " + type);
                // Use a default colored rectangle if image is not found
                BufferedImage defaultImg = new BufferedImage(40, 10, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = defaultImg.createGraphics();
                g.setColor(Color.ORANGE);
                g.fillRect(0, 0, 40, 40);
                g.dispose();
                furnitureIcons.put(type, new ImageIcon(defaultImg));
            }
        }
        
        // Load fixture icons
        String[] fixtureTypes = {"commode", "washbasin", "shower", "kitchen_sink", "stove"};
        for (String type : fixtureTypes) {
            try {
                BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/fixtures/" + type + ".png"));
                // Use better quality image scaling
                BufferedImage scaledImg = new BufferedImage(40, 10, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = scaledImg.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(img, 0, 0, 40, 10, null);
                g2d.dispose();
                fixtureIcons.put(type, new ImageIcon(scaledImg));
            } catch (IOException e) {
                System.err.println("Could not load fixture icon: " + type);
                // Use a default colored rectangle if image is not found
                BufferedImage defaultImg = new BufferedImage(40, 10, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = defaultImg.createGraphics();
                g.setColor(Color.CYAN);
                g.fillRect(0, 0, 40, 10);
                g.dispose();
                fixtureIcons.put(type, new ImageIcon(defaultImg));
            }
        }
    }
    
    public void initializeComponents() {
        // Initialize Control Panel Components
        controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(250, CANVAS_HEIGHT));
        controlPanel.setBorder(new TitledBorder("Controls"));
        
        roomTypeCombo = new JComboBox<>(new String[]{"Bedroom", "Bathroom", "Kitchen", "Living Room"});
        widthField = new JTextField("200");
        heightField = new JTextField("200");
        directionCombo = new JComboBox<>(new String[]{"East", "South", "North", "West"});
        alignmentCombo = new JComboBox<>(new String[]{"Left", "Center", "Right"});
        
        // Initialize furniture and fixtures panels
        furniturePanel = new JPanel();
        furniturePanel.setBorder(new TitledBorder("Furniture"));
        furniturePanel.setLayout(new GridLayout(0, 1, 5, 5));
        
        fixturesPanel = new JPanel();
        fixturesPanel.setBorder(new TitledBorder("Fixtures"));
        fixturesPanel.setLayout(new GridLayout(0, 1, 5, 5));
        
        // Add furniture buttons
        for (Map.Entry<String, ImageIcon> entry : furnitureIcons.entrySet()) {
            JButton btn = new JButton(entry.getKey(), entry.getValue());
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.addActionListener(e -> {
                selectedFurniture = entry.getKey();
                selectedFixture = null;
            });
            furniturePanel.add(btn);
        }
        
        // Add fixture buttons
        for (Map.Entry<String, ImageIcon> entry : fixtureIcons.entrySet()) {
            JButton btn = new JButton(entry.getKey(), entry.getValue());
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.addActionListener(e -> {
                selectedFixture = entry.getKey();
                selectedFurniture = null;
            });
            fixturesPanel.add(btn);
        }

        // Initialize Canvas
        canvas = new DrawingCanvas();
    }
    // Save the floor plan
    public void savePlan() {
        JFileChooser file = new JFileChooser();
        file.setCurrentDirectory(new File("."));
        file.setDialogTitle("Save Floor Plan");
        //int response = file.showSaveDialog(this);

        //if(response == JFileChooser.APPROVE_OPTION){
            //File fileselected = file.getSelectedFile();
            //String filename = fileselected.getAbsolutePath();
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("floorplan.ser"))){
                oos.writeObject(rooms);
                oos.writeObject(furnitureItems);
                oos.close();
                JOptionPane.showMessageDialog(this, "Plan saved successfully!");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving the plan."+e.getMessage());
            }
        //}
    }


    // loading the floor plan
    @SuppressWarnings("unchecked")
    public void loadPlan() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));
        fileChooser.setDialogTitle("Open Floor Plan");
    
        // filtering .ser files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Serialized Files (*.ser)", "ser");
        fileChooser.setFileFilter(filter);
    
        int userSelection = fileChooser.showOpenDialog(this);
    
        if (userSelection == JFileChooser.APPROVE_OPTION) {
        //File fileToLoad = fileChooser.getSelectedFile();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("floorplan.ser"))) { //should be fileToLoad here
            rooms = (ArrayList<Room>) ois.readObject();
            furnitureItems = (ArrayList<FurnitureItem>) ois.readObject();
            canvas.repaint();
            JOptionPane.showMessageDialog(this, "Plan loaded successfully!");
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading plan."+e.getMessage());
        }
        }
    }

    // Export the floor plan as an image
    public void exportAsImage() {
        BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        canvas.paint(g2d);
        g2d.dispose();
        try {
            ImageIO.write(image, "png", new File("floorplan.png"));
            JOptionPane.showMessageDialog(this, "Plan exported as image successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting image.");
        }
    }

    public void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Layout Control Panel
        controlPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(1, 5, 2, 5); //control panel dimensions for each components
        
        controlPanel.add(new JLabel("Room Type:"), gbc);
        gbc.gridy++;
        controlPanel.add(roomTypeCombo, gbc);
        
        gbc.gridy++;
        controlPanel.add(new JLabel("Width (pixels):"), gbc);
        gbc.gridy++;
        controlPanel.add(widthField, gbc);
        
        gbc.gridy++;
        controlPanel.add(new JLabel("Height (pixels):"), gbc);
        gbc.gridy++;
        controlPanel.add(heightField, gbc);
        
        gbc.gridy++;
        controlPanel.add(new JLabel("Direction:"), gbc);
        gbc.gridy++;
        controlPanel.add(directionCombo, gbc);
        
        gbc.gridy++;
        controlPanel.add(new JLabel("Alignment:"), gbc);
        gbc.gridy++;
        controlPanel.add(alignmentCombo, gbc);
        
        JButton addRoomButton = new JButton("Add Room");
        gbc.gridy++;
        controlPanel.add(addRoomButton, gbc);

        JButton rotateButton = new JButton("Rotate Room"); 
        gbc.gridy++;
        controlPanel.add(rotateButton, gbc);

        JButton removeRoomButton = new JButton("Remove Room"); 
        gbc.gridy++; 
        controlPanel.add(removeRoomButton, gbc);
        
        JButton addDoorButton = new JButton("Add Door");
        gbc.gridy++;
        controlPanel.add(addDoorButton, gbc);
        
        JButton addWindowButton = new JButton("Add Window");
        gbc.gridy++;
        controlPanel.add(addWindowButton, gbc);
        
        // Inside initializeComponents() method
        JButton saveButton = new JButton("Save Plan");
        gbc.gridy++;
        controlPanel.add(saveButton, gbc);

        JButton loadButton = new JButton("Load Plan");
        gbc.gridy++;
        controlPanel.add(loadButton, gbc);

        JButton exportButton = new JButton("Export as Image");
        gbc.gridy++;
        controlPanel.add(exportButton, gbc);

        // Add furniture and fixtures panels to control panel
        gbc.gridy++;
        controlPanel.add(furniturePanel, gbc);
        gbc.gridy++;
        controlPanel.add(fixturesPanel, gbc);
        
        // Add button listeners
        saveButton.addActionListener(e -> savePlan());
        loadButton.addActionListener(e -> loadPlan());
        exportButton.addActionListener(e -> exportAsImage());

        // Add Button Listeners
        addRoomButton.addActionListener(e -> addRoom());
        rotateButton.addActionListener(e -> rotateSelectedRoom());
        removeRoomButton.addActionListener(e -> removeRoom());

        addDoorButton.addActionListener(e -> {
            isAddingDoor = true;
            isAddingWindow = false;
            int response = JOptionPane.showOptionDialog(null, "Choose door orientation",
                "Door Orientation",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new String[]{"Horizontal", "Vertical"}, "Horizontal");
            isVerticalDoor = (response == 1);
        });        
        
        addWindowButton.addActionListener(e -> {
            isAddingWindow = true;
            isAddingDoor = false;
            int response = JOptionPane.showOptionDialog(null, "Choose window orientation",
                "Window Orientation",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new String[]{"Horizontal", "Vertical"}, "Horizontal");
            isVerticalWindow = (response == 1);
        });
        
        
        // Add panels to frame
        add(controlPanel, BorderLayout.WEST);
        add(canvas, BorderLayout.CENTER);
    }
    
    public void addRoom() {
        int width = Integer.parseInt(widthField.getText());
        int height = Integer.parseInt(heightField.getText());
        String type = (String) roomTypeCombo.getSelectedItem();
        
        Color roomColor;
        switch(type) {
            case "Bedroom": roomColor = BEDROOM_COLOR; break;
            case "Bathroom": roomColor = BATHROOM_COLOR; break;
            case "Kitchen": roomColor = KITCHEN_COLOR; break;
            default: roomColor = LIVING_COLOR;
        }
        
        Room newRoom;
        if (referenceRoom == null) {           
            newRoom = new Room(
                // (CANVAS_WIDTH - width) / 2,
                // (CANVAS_HEIGHT - height) / 2,
                0,0,
                width, height, type, roomColor
            );
        } else {
            // Place room relative to reference room
            String direction = (String) directionCombo.getSelectedItem();
            String alignment = (String) alignmentCombo.getSelectedItem();
            Point position = calculatePosition(referenceRoom, direction, alignment, width, height);
            // Room lastRoom = rooms.get(rooms.size() - 1); 
            // int newX = lastRoom.x + lastRoom.width; 
            // int newY = lastRoom.y;
            if (position.x + width >= 1300) { 
                //JOptionPane.showMessageDialog(this, "Cannot add room here - exceeds canvas width!"); 
                position.x = 0;
                int temp=0;
                int temp2 = position.y;
                for (Room room : rooms) {
                    if (temp <= room.height && temp2 == room.y) {
                        temp = room.height;
                    }
                }
                //height = temp;
                position.y = position.y+temp;
                if (position.y + height >= 850){
                    JOptionPane.showMessageDialog(this, "Cannot add room here - exceeds canvas dimensions!"); 
                    return;
                }
                //return; 
            }
            
            newRoom = new Room(position.x, position.y, width, height, type, roomColor);
            
            // Check for overlap
            if (checkOverlap(newRoom)) {
                JOptionPane.showMessageDialog(this, "Room overlaps with existing rooms!");
                return;
            }
        }
        
        rooms.add(newRoom);
        selectedRoom = newRoom;
        referenceRoom = newRoom;
        canvas.repaint();
    }
    
    public void removeRoom() { 
        if (selectedRoom != null) { 
            rooms.remove(selectedRoom); 
            // selectedRoom = null; 
            // referenceRoom = null;
            for (Room room : rooms) {
                selectedRoom = room;
                referenceRoom = room;
            } 
            canvas.repaint(); 
        } else { 
            JOptionPane.showMessageDialog(this, "No room selected to remove!"); 
        } 
    }

    public Point calculatePosition(Room reference, String direction, String alignment, int width, int height) {
        int x = reference.x;
        int y = reference.y;
        
        switch(direction) {
            case "North":
                y = reference.y - height;
                switch(alignment) {
                    case "Left": x = reference.x; break;
                    case "Center": x = reference.x + (reference.width - width) / 2; break;
                    case "Right": x = reference.x + reference.width - width; break;
                }
                break;
            case "South":
                y = reference.y + reference.height;
                switch(alignment) {
                    case "Left": x = reference.x; break;
                    case "Center": x = reference.x + (reference.width - width) / 2; break;
                    case "Right": x = reference.x + reference.width - width; break;
                }
                break;
            case "East":
                x = reference.x + reference.width;
                switch(alignment) {
                    case "Left": y = reference.y; break;
                    case "Center": y = reference.y + (reference.height - height) / 2; break;
                    case "Right": y = reference.y + reference.height - height; break;
                }
                break;
            case "West":
                x = reference.x - width;
                switch(alignment) {
                    case "Left": y = reference.y; break;
                    case "Center": y = reference.y + (reference.height - height) / 2; break;
                    case "Right": y = reference.y + reference.height - height; break;
                }
                break;
        }
        
        // Snap to grid
        //x = Math.round(x / drag) * drag;
        //y = Math.round(y / drag) * drag;
        
        return new Point(x, y);
    }
    
    public boolean checkOverlap(Room newRoom) {
        for (Room room : rooms) {
            if (room != newRoom && room.intersects(newRoom)) {
                return true;
            }
        }
        return false;
    }

    public void rotateSelectedRoom() {
        if (selectedRoom != null) {
            // Save original dimensions and position
            int originalWidth = selectedRoom.width;
            int originalHeight = selectedRoom.height;
            int originalX = selectedRoom.x;
            int originalY = selectedRoom.y;
    
            // Rotate the room
            selectedRoom.rotate();
    
            // Check for overlaps
            boolean overlap = false;
            for (Room room : rooms) {
                if (room != selectedRoom && room.intersects(selectedRoom)) {
                    overlap = true;
                    break;
                }
            }
    
            // Revert if overlap detected
            if (overlap) {
                selectedRoom.width = originalWidth;
                selectedRoom.height = originalHeight;
                selectedRoom.x = originalX;
                selectedRoom.y = originalY;
                JOptionPane.showMessageDialog(this, "Cannot rotate room - overlap detected!");
            }
    
            canvas.repaint();
        }
    }

    public boolean hasAdjacentRoom(Room room, Point p, boolean isVertical) {
        for (Room adjacentRoom : rooms) {
            if (adjacentRoom != room) {
                if (isVertical) {
                    // Check left and right sides for adjacent rooms
                    if ((p.x == room.x && p.x == adjacentRoom.x + adjacentRoom.width && p.y >= adjacentRoom.y && p.y <= adjacentRoom.y + adjacentRoom.height) || 
                        (p.x == room.x + room.width && p.x == adjacentRoom.x && p.y >= adjacentRoom.y && p.y <= adjacentRoom.y + adjacentRoom.height)) {
                        return true;
                    }
                } else {
                    // Check top and bottom sides for adjacent rooms
                    if ((p.y == room.y && p.y == adjacentRoom.y + adjacentRoom.height && p.x >= adjacentRoom.x && p.x <= adjacentRoom.x + adjacentRoom.width) || 
                        (p.y == room.y + room.height && p.y == adjacentRoom.y && p.x >= adjacentRoom.x && p.x <= adjacentRoom.x + adjacentRoom.width)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    

    class FurnitureItem implements Serializable {
        int x, y;
        String type;
        Dimension size;
        boolean isFixture;
        int rotation = 0; // 0, 90, 180, or 270 degrees
        
        public FurnitureItem(int x, int y, String type, boolean isFixture) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.isFixture = isFixture;
            this.size = isFixture ? FIXTURE_DIMENSIONS.get(type) : FURNITURE_DIMENSIONS.get(type);
        }
        public void rotate() {
            rotation = (rotation + 90) % 360;
            // Swap width and height
            int temp = size.width;
            size.width = size.height;
            size.height = temp;
        }
        public Rectangle getBounds() {
            return new Rectangle(x, y, size.width, size.height);
        }
        public boolean contains(Point p) {
            return p.x >= x && p.x <= x + size.width && 
                   p.y >= y && p.y <= y + size.height;
        }
        public boolean intersects(FurnitureItem other) {
            return getBounds().intersects(other.getBounds());
        }
        public void draw(Graphics2D g2d) {
            // Draw with high quality
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Save the current transform
            AffineTransform oldTransform = g2d.getTransform();

            // Translate to rotation point (center of the item)
            g2d.translate(x + size.width/2, y + size.height/2);
            g2d.rotate(Math.toRadians(rotation));
            g2d.translate(-size.width/2, -size.height/2);

            // Draw the furniture/fixture
            ImageIcon icon = isFixture ? fixtureIcons.get(type) : furnitureIcons.get(type);
            if (icon != null) {
                g2d.drawImage(icon.getImage(), 0, 0, size.width, size.height, null);
            } else {
                // Fallback to colored rectangle
                g2d.setColor(isFixture ? Color.CYAN : Color.ORANGE);
                g2d.fillRect(x, y, size.width, size.height);
            }
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.drawRect(0, 0, size.width, size.height);

            // Restore the original transform
            g2d.setTransform(oldTransform);
        }
    }
    
    class Room implements Serializable {
        int x, y, width, height;
        String type;
        Color color;
        ArrayList<Door> doors;
        ArrayList<Window> windows;
        
        public Room(int x, int y, int width, int height, String type, Color color) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
            this.color = color;
            this.doors = new ArrayList<>();
            this.windows = new ArrayList<>();
        }
        
        public boolean contains(Point p) {
            return p.x >= x && p.x <= x + width && p.y >= y && p.y <= y + height;
        }
        
        public boolean intersects(Room other) {
            return !(x + width <= other.x || other.x + other.width <= x ||
                    y + height <= other.y || other.y + other.height <= y);
        }
        
        public void draw(Graphics2D g2d) {
            // Draw room
            g2d.setColor(color);
            g2d.fillRect(x, y, width, height);
            
            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            //g2d.drawRect(x, y, width, height);
            drawWalls(g2d);
            
            // draw doors
            // g2d.setColor(Color.BLACK);
            // for (Door door : doors) {
            //     door.draw(g2d, x, y);
            // }
            
            // draw windows
            //g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2d.setColor(Color.BLUE);
            for (Window window : windows) {
                window.draw(g2d,x, y);
            }
            
            // draw room type
            g2d.setColor(Color.BLACK);
            g2d.drawString(type, x + 5, y + 20);
        }
        public void rotate() {
            // Swap width and height
            int temp = width;
            width = height;
            height = temp;
        }        

        public void drawWalls(Graphics2D g2d) { 

            // top wall 
            if (!hasDoorOnWall(new Point(x, y), new Point(x + width, y))) { 
                g2d.drawLine(x, y, x + width, y); 
            } 
            // bottom wall 
            if (!hasDoorOnWall(new Point(x, y + height), new Point(x + width, y + height))) { 
                g2d.drawLine(x, y + height, x + width, y + height); 
            } 
            // left wall 
            if (!hasDoorOnWall(new Point(x, y), new Point(x, y + height))) { 
                g2d.drawLine(x, y, x, y + height); 
            } 
            // right wall 
            if (!hasDoorOnWall(new Point(x + width, y), new Point(x + width, y + height))) { 
                g2d.drawLine(x + width, y, x + width, y + height); 
            }
        }
        // public boolean hasDoor(Point start, Point end) { 
        //     for (Door door : doors) { 
        //         if (door.intersects(start, end, x, y)) { 
        //             return true; 
        //         } 
        //     }  
        //     return false; 
        // }
        public boolean hasDoorOnWall(Point start, Point end) { 
            final int WALL_TOLERANCE = 5;
            //final int PERPENDICULAR_TOLERANCE = 3;
            // Check doors in this room
            for (Door door : doors) { 
                if (door.intersects(start, end, x, y)) { 
                    return true; 
                } 
            }
    
            // Check doors in adjacent rooms that share this wall
            for (Room otherRoom : FloorPlanner.this.rooms) {
                if (otherRoom != this) {
                    // Check if rooms share a wall
                    boolean isSharedWall = false;
                    Point otherStart = null;
                    Point otherEnd = null;
    
                    // Horizontal wall check
                    if (Math.abs(start.y - otherRoom.y) <= WALL_TOLERANCE || 
                        Math.abs(start.y - (otherRoom.y + otherRoom.height)) <= WALL_TOLERANCE) {

                        // Check if x-coordinates overlap
                        if (start.x < otherRoom.x + otherRoom.width && 
                            end.x > otherRoom.x) {
                            isSharedWall = true;
                            if (Math.abs(start.y - otherRoom.y) <= WALL_TOLERANCE) {
                                otherStart = new Point(otherRoom.x, otherRoom.y);
                                otherEnd = new Point(otherRoom.x + otherRoom.width, otherRoom.y);
                            } else {
                                otherStart = new Point(otherRoom.x, otherRoom.y + otherRoom.height);
                                otherEnd = new Point(otherRoom.x + otherRoom.width, otherRoom.y + otherRoom.height);
                            }
                        }
                    }
                    // Vertical wall check
                    else if (Math.abs(start.x - otherRoom.x) <= WALL_TOLERANCE || 
                             Math.abs(start.x - (otherRoom.x + otherRoom.width)) <= 5) {
                        // Check if y-coordinates overlap
                        if (start.y < otherRoom.y + otherRoom.height && 
                            end.y > otherRoom.y) {
                            isSharedWall = true;
                            if (Math.abs(start.x - otherRoom.x) <= WALL_TOLERANCE) {
                                otherStart = new Point(otherRoom.x, otherRoom.y);
                                otherEnd = new Point(otherRoom.x, otherRoom.y + otherRoom.height);
                            } else {
                                otherStart = new Point(otherRoom.x + otherRoom.width, otherRoom.y);
                                otherEnd = new Point(otherRoom.x + otherRoom.width, otherRoom.y + otherRoom.height);
                            }
                        }
                    }
    
                    if (isSharedWall) {
                        for (Door door : otherRoom.doors) {
                            if (door.intersects(otherStart, otherEnd, otherRoom.x, otherRoom.y)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }
    class Window implements Serializable {
        int offsetX, offsetY;
        boolean isVertical;
    
        public Window(int offsetX, int offsetY, boolean isVertical) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.isVertical = isVertical;
        }
    
        public void draw(Graphics2D g2d, int roomX, int roomY) {
            int length = 15; // Window length
            int startX = roomX + offsetX;
            int startY = roomY + offsetY;
            g2d.setStroke(new BasicStroke(5)); // Set the thickness here
            if (isVertical) {
                g2d.drawLine(startX, startY - length / 2, startX, startY + length / 2);
            } else {
                g2d.drawLine(startX - length / 2, startY, startX + length / 2, startY);
            }
        }
        public boolean intersects(Point start, Point end,int roomX, int roomY) {
            int windowStartX = roomX + offsetX;
            int windowStartY = roomY + offsetY;
            int windowEndX = isVertical ? windowStartX : windowStartX + 15;
            int windowEndY = isVertical ? windowStartY + 15 : windowStartY;
            
            if (isVertical) {
                return Math.abs(start.x - (windowStartX)) <= 5 &&
                       (start.y <= (windowStartY) && end.y >= (windowEndY));
            } else {
                return Math.abs(start.y - (windowStartY)) <= 5 &&
                       (start.x <= (windowStartX) && end.x >= (windowEndX));
            }
        }
        
    }
    
    class Door implements Serializable {
        int offsetX, offsetY;
        boolean isVertical;
        int length;

        public Door(int offsetX, int offsetY, boolean isVertical, int length) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.isVertical = isVertical;
            this.length = length;
        }
    
        // public void draw(Graphics2D g2d, int roomX, int roomY) {
        //     //int length = 40; // Door length
        //     int startX = roomX + offsetX;
        //     int startY = roomY + offsetY;
        //     g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{10}, 0)); // Dashed line
        //     if (isVertical) {
        //         g2d.drawLine(startX, startY - length / 4, startX, startY + length / 4);
        //     } else {
        //         g2d.drawLine(startX - length / 4, startY, startX + length / 4, startY);
        //     }
        // }
        public boolean intersects(Point start, Point end,int roomX, int roomY) {
            int doorStartX = roomX + offsetX;
            int doorStartY = roomY + offsetY;
            int doorEndX = isVertical ? doorStartX : doorStartX + 15;
            int doorEndY = isVertical ? doorStartY + 15 : doorStartY;
        
            if (isVertical) {
                return Math.abs(start.x - (doorStartX)) <= 10 &&
                       (start.y <= (doorStartY) && end.y >= (doorEndY));
            } else {
                return Math.abs(start.y - (doorStartY)) <= 10 &&
                       (start.x <= (doorStartX) && end.x >= (doorEndX));
            }
        }
    }

    
    
    class DrawingCanvas extends JPanel {
        public static final int DOT_SIZE = 2;
        
        public int getWallLength(Room room, Point p, boolean isVertical) {
            if (isVertical) {
                return room.height;
            } else {
                return room.width;
            }
        }
        
        
        public boolean isValidWindowPosition(Room room, Point p) {
            int tolerance = 3;
            
            // Check if point is on a wall
            boolean isOnWall = (Math.abs(p.x - room.x) <= tolerance || 
                               Math.abs(p.x - (room.x + room.width)) <= tolerance ||
                               Math.abs(p.y - room.y) <= tolerance || 
                               Math.abs(p.y - (room.y + room.height)) <= tolerance);
            
            if (!isOnWall) {
                return false;
            }
            
            // Check if the window would be between rooms
            if ((Math.abs(p.x - room.x) <= tolerance && hasAdjacentRoom(room, "left")) ||
                (Math.abs(p.x - (room.x + room.width)) <= tolerance && hasAdjacentRoom(room, "right")) ||
                (Math.abs(p.y - room.y) <= tolerance && hasAdjacentRoom(room, "top")) ||
                (Math.abs(p.y - (room.y + room.height)) <= tolerance && hasAdjacentRoom(room, "bottom"))) {
                JOptionPane.showMessageDialog(FloorPlanner.this, "Windows cannot be placed between rooms!");
                return false;
            }
            
            return true;
        }
        // public boolean isValidDoorPosition(Room room, Point p) {
        //     int tolerance = 5; // Allowable distance from the wall for placing doors
        //     return (Math.abs(p.x - room.x) <= tolerance || Math.abs(p.x - (room.x + room.width)) <= tolerance ||
        //             Math.abs(p.y - room.y) <= tolerance || Math.abs(p.y - (room.y + room.height)) <= tolerance);
        // }        

        public boolean isValidDoorPosition(Room room, Point p) {
            int tolerance = 5;
            
            // Only check outer wall restrictions for bedrooms and bathrooms
            if ((room.type.equals("Bedroom") || room.type.equals("Bathroom"))) {
                // Check if the door is being placed on an outer wall
                boolean isOuterWall = 
                    (Math.abs(p.x - room.x) <= tolerance && !hasAdjacentRoom(room, "left")) ||
                    (Math.abs(p.x - (room.x + room.width)) <= tolerance && !hasAdjacentRoom(room, "right")) ||
                    (Math.abs(p.y - room.y) <= tolerance && !hasAdjacentRoom(room, "top")) ||
                    (Math.abs(p.y - (room.y + room.height)) <= tolerance && !hasAdjacentRoom(room, "bottom"));
    
                if (isOuterWall) {
                    JOptionPane.showMessageDialog(FloorPlanner.this, 
                        room.type + " cannot have doors facing outside!");
                    return false;
                }
            }
            
            // For all rooms, check if the point is on any wall
            return (Math.abs(p.x - room.x) <= tolerance || 
                    Math.abs(p.x - (room.x + room.width)) <= tolerance || 
                    Math.abs(p.y - room.y) <= tolerance || 
                    Math.abs(p.y - (room.y + room.height)) <= tolerance);
        }

        public boolean hasAdjacentRoom(Room currentRoom, String direction) {
            int tolerance = 5;
            
            for (Room room : rooms) {
                if (room == currentRoom) continue;
                
                switch (direction) {
                    case "left":
                        if (Math.abs((currentRoom.x) - (room.x + room.width)) <= tolerance &&
                            currentRoom.y < room.y + room.height &&
                            currentRoom.y + currentRoom.height > room.y) {
                            return true;
                        }
                        break;
                    case "right":
                        if (Math.abs((currentRoom.x + currentRoom.width) - room.x) <= tolerance &&
                            currentRoom.y < room.y + room.height &&
                            currentRoom.y + currentRoom.height > room.y) {
                            return true;
                        }
                        break;
                    case "top":
                        if (Math.abs(currentRoom.y - (room.y + room.height)) <= tolerance &&
                            currentRoom.x < room.x + room.width &&
                            currentRoom.x + currentRoom.width > room.x) {
                            return true;
                        }
                        break;
                    case "bottom":
                        if (Math.abs((currentRoom.y + currentRoom.height) - room.y) <= tolerance &&
                            currentRoom.x < room.x + room.width &&
                            currentRoom.x + currentRoom.width > room.x) {
                            return true;
                        }
                        break;
                }
            }
            return false;
        }

        
        public boolean isDoorOverlap(Room room, Point p, boolean isVertical) {
            int doorLength = getWallLength(room, p, isVertical);
            int startX = p.x;
            int startY = p.y;
        
            for (Door door : room.doors) {
                if (isOverlap(startX, startY, door.offsetX + room.x, door.offsetY + room.y, doorLength, isVertical)) {
                    return true;
                }
            }
        
            for (Window window : room.windows) {
                if (isOverlap(startX, startY, window.offsetX + room.x, window.offsetY + room.y, 30, window.isVertical)) {
                    return true;
                }
            }
        
            return false;
        }       
        
        public boolean isWindowOverlap(Room room, Point p, boolean isVertical) {
            int windowLength = 30; // Length of the window
            int startX = p.x;
            int startY = p.y;
        
            for (Window window : room.windows) {
                if (isOverlap(startX, startY, window.offsetX + room.x, window.offsetY + room.y, windowLength, isVertical)) {
                    return true;
                }
            }
        
            for (Door door : room.doors) {
                if (isOverlap(startX, startY, door.offsetX + room.x, door.offsetY + room.y, getWallLength(room, p, door.isVertical), door.isVertical)) {
                    return true;
                }
            }
        
            return false;
        }        

        public boolean isOverlap(int startX1, int startY1, int startX2, int startY2, int length, boolean isVertical) {
            if (isVertical) {
                return Math.abs(startX1 - startX2) <= 5 &&
                       ((startY1 >= startY2 && startY1 <= startY2 + length) ||
                        (startY2 >= startY1 && startY2 <= startY1 + length));
            } else {
                return Math.abs(startY1 - startY2) <= 5 &&
                       ((startX1 >= startX2 && startX1 <= startX2 + length) ||
                        (startX2 >= startX1 && startX2 <= startX1 + length));
            }
        }

        public DrawingCanvas() {
            setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
            setBackground(Color.LIGHT_GRAY);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point p = e.getPoint();
                    Point p1 = new Point(); 

                    // Handle right-click for rotation
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        for (FurnitureItem item : furnitureItems) {
                            if (item.contains(p)) {
                                item.rotate();
                                repaint();
                                break;
                            }
                        }
                        return;
                    }

                    if (isAddingDoor || isAddingWindow) {
                        for (Room room : rooms) {
                            if (room.contains(p)) {
                                if (isAddingDoor) {
                                    if (isValidDoorPosition(room, p) && !isDoorOverlap(room, p, isVerticalDoor)) {
                                        int offsetX = p.x - room.x; 
                                        int offsetY = p.y - room.y; 
                                        int length = getWallLength(room, p, isVerticalDoor);
                                        room.doors.add(new Door(offsetX, offsetY, isVerticalDoor, length));
                                        
                                    } else {
                                        JOptionPane.showMessageDialog(FloorPlanner.this, "Doors must be placed on the walls only & Should Not overlap with existing Doors!");
                                    }
                                } 
                                else if (isAddingWindow) { 
                                    if (isValidWindowPosition(room, p) && !isWindowOverlap(room, p, isVerticalWindow)) { 
                                        int offsetX = p.x - room.x;
                                        int offsetY = p.y - room.y;
                                        room.windows.add(new Window(offsetX, offsetY, isVerticalWindow)); 
                                    } else {
                                        JOptionPane.showMessageDialog(FloorPlanner.this, "Windows must be placed on the walls only & Should Not overlap with existing Windows!"); 
                                    } 
                                } 
                                repaint(); 
                                break;
                            }
                        }
                        isAddingDoor = false;
                        isAddingWindow = false;
                    } else {
                        // Select room for dragging
                        for (Room room : rooms) {
                            if (room.contains(p)) {
                                selectedRoom = room;
                                referenceRoom = room;
                                draggedRoom = room;
                                p1.x = room.x;
                                p1.y = room.y;

                                dragStart = p1; //the point from which dragging will actually start from
                                break;
                            }
                        }
                    }
                    if (selectedFurniture != null || selectedFixture != null) {
                        String type = selectedFurniture != null ? selectedFurniture : selectedFixture;
                        boolean isFixture = selectedFixture != null;
                        
                        // Check if clicked point is inside a room
                        boolean inRoom = false;
                        for (Room room : rooms) {
                            if (room.contains(p)) {
                                inRoom = true;

                                // Check for overlap with existing items 
                                FurnitureItem newItem = new FurnitureItem(p.x, p.y, type, isFixture); 
                                boolean overlap = false; 
                                for (FurnitureItem item : furnitureItems) { 
                                    if (item.intersects(newItem)) { 
                                        overlap = true; 
                                        break; 
                                    } 
                                }
                                if (overlap) { 
                                    JOptionPane.showMessageDialog(FloorPlanner.this, "Cannot place overlapping furniture or fixtures!"); 
                                } else { 
                                    // Add new furniture/fixture 
                                    furnitureItems.add(newItem); 
                                    selectedFurniture = null; 
                                    selectedFixture = null; 
                                    repaint(); 
                                } 
                                break;
                            }
                        }
                        
                        if (!inRoom) {
                            JOptionPane.showMessageDialog(FloorPlanner.this, 
                                "Furniture and fixtures must be placed inside rooms!");
                        }
                    } else {
                        // Check for existing furniture/fixture dragging
                        for (FurnitureItem item : furnitureItems) {
                            if (item.contains(p)) {
                                draggedItemStart = p;
                                break;
                            }
                        }
                        
                        // ... (keep existing room selection code)
                        for (Room room : rooms) {
                            if (room.contains(p)) {
                                selectedRoom = room;
                                referenceRoom = room;
                                draggedRoom = room;
                                p1.x = room.x;
                                p1.y = room.y;

                                dragStart = p1; //the point from which dragging will actually start from
                                break;
                            }
                        }
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedRoom != null) {
                        // Snap to grid
                        draggedRoom.x = Math.round(draggedRoom.x / drag) * drag;
                        draggedRoom.y = Math.round(draggedRoom.y / drag) * drag;
                        
                        boolean outsideCanvas = draggedRoom.x < 0 || draggedRoom.y < 0 || draggedRoom.x + draggedRoom.width > getWidth() || draggedRoom.y + draggedRoom.height > getHeight();

                        // Check overlap
                        boolean overlap = false;
                        for (Room room : rooms) {
                            if (room != draggedRoom && room.intersects(draggedRoom)) {
                                overlap = true;
                                break;
                            }
                        }
                        
                        if (outsideCanvas || overlap) {
                            // Revert position
                            draggedRoom.x = Math.round(dragStart.x / drag) * drag;
                            draggedRoom.y = Math.round(dragStart.y / drag) * drag;
                            if (outsideCanvas) { 
                                JOptionPane.showMessageDialog(FloorPlanner.this, "Cannot place room outside the canvas!"); 
                            } else if (overlap) { 
                                JOptionPane.showMessageDialog(FloorPlanner.this, "Cannot place room here - overlap detected!"); 
                            }
                        }
                        
                        draggedRoom = null;
                        dragStart = null;
                        draggedItemStart = null;
                        repaint();
                    }
                    // Snap dragged item to grid 
                    if (draggedItemStart != null) { 
                        for (FurnitureItem item : furnitureItems) { 
                            if (item.contains(draggedItemStart)) { 
                                Point p = e.getPoint(); 
                                item.x = Math.round(p.x / drag) * drag; 
                                item.y = Math.round(p.y / drag) * drag; 
                                
                                // Check for overlaps and revert if necessary 
                                boolean overlap = false; 
                                for (FurnitureItem otherItem : furnitureItems) { 
                                    if (otherItem != item && item.intersects(otherItem)) { 
                                        overlap = true; 
                                        break; 
                                    } 
                                } 
                                if (overlap) { 
                                    // Revert position if overlap detected 
                                    item.x = Math.round((draggedItemStart.x - p.x + item.x) / drag) * drag;
                                    item.y = Math.round((draggedItemStart.y - p.y + item.y) / drag) * drag; 
                                    JOptionPane.showMessageDialog(FloorPlanner.this, "Cannot place overlapping furniture or fixture!"); 
                                } 
                                draggedItemStart = null; 
                                repaint();
                            }
                        }
                    }       
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedItemStart != null) {
                        Point p = e.getPoint();
                        int dx = p.x - draggedItemStart.x;
                        int dy = p.y - draggedItemStart.y;
                        
                        // Update furniture position
                        for (FurnitureItem item : furnitureItems) {
                            if (item.contains(draggedItemStart)) {
                                item.x += dx;
                                item.y += dy;

                                boolean overlap = false; 
                                for (FurnitureItem otherItem : furnitureItems) { 
                                    if (otherItem != item && item.intersects(otherItem)) { 
                                        overlap = true; 
                                        break; 
                                    } 
                                } if (overlap) { 
                                    // Revert position if overlap detected 
                                    item.x -= dx; 
                                    item.y -= dy; 
                                } else { 
                                    draggedItemStart = p; 
                                }

                                //draggedItemStart = p;
                                repaint();
                                break;
                            }
                        }
                    }
                    if (draggedRoom != null) {
                        Point p = e.getPoint();
                        int dx = p.x - dragStart.x;
                        int dy = p.y - dragStart.y;
                        
                        draggedRoom.x = Math.round((dragStart.x + dx) / drag) * drag;
                        draggedRoom.y = Math.round((dragStart.y + dy) / drag) * drag;
                        
                        repaint();
                    }
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw grid
            g2d.setColor(Color.BLACK);
            for (int x = 0; x < getWidth(); x += GRID_SIZE) {
                for (int y = 0; y < getHeight(); y += GRID_SIZE) {
                    g2d.fillOval(x - DOT_SIZE/2, y - DOT_SIZE/2, DOT_SIZE, DOT_SIZE);
                }
            }
            
            // Draw rooms
            for (Room room : rooms) {
                room.draw(g2d);
            }

            // Draw furniture and fixtures
            for (FurnitureItem item : furnitureItems) {
                item.draw(g2d);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FloorPlanner());
    }
}   