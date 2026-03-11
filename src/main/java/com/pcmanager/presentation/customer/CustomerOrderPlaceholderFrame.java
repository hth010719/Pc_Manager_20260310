package com.pcmanager.presentation.customer;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class CustomerOrderPlaceholderFrame extends JFrame {
    private static final String CATEGORY_ALL = "전체";
    private static final Color CARD_BORDER_COLOR = new Color(215, 205, 190);
    private static final Color IMAGE_BG_COLOR = new Color(239, 234, 225);
    private static final Color IMAGE_BORDER_COLOR = new Color(180, 168, 150);
    private static final Path MENU_IMAGE_DIR = resolveMenuImageDir();

    private final JPanel menuGridPanel = new JPanel();
    private final JPanel cartItemsPanel = new JPanel();
    private final JLabel headerLabel = new JLabel();
    private final JLabel cartTotalLabel = new JLabel();
    private final JScrollPane menuScrollPane;
    private final PcSocketClient socketClient;
    private final Long seatId;
    private final Map<MenuItemData, Integer> cartItems = new LinkedHashMap<>();
    private final Map<String, ImageIcon> scaledImageCache = new LinkedHashMap<>();

    private final List<MenuItemData> menuItems = List.of(
            new MenuItemData(1L, "식사류", "스팸 마요 덮밥", 6900, "짭조름한 스팸과 고소한 마요를 듬뿍 올린 든든한 한 끼"),
            new MenuItemData(2L, "식사류", "치킨 마요 덮밥", 7200, "바삭한 치킨과 달콤한 소스로 부담 없이 즐기는 인기 메뉴"),
            new MenuItemData(3L, "식사류", "김치볶음밥", 6800, "매콤하게 볶아낸 김치와 고슬한 밥이 잘 어울리는 메뉴"),
            new MenuItemData(4L, "식사류", "삼겹살 정식", 8900, "구운 삼겹살과 밥, 반찬을 함께 즐기는 묵직한 정식"),
            new MenuItemData(5L, "면류", "짜계치", 5200, "짜장과 계란, 치즈가 어우러진 진한 풍미의 라면"),
            new MenuItemData(6L, "면류", "라볶이", 5900, "쫄깃한 떡과 라면을 매콤달콤하게 끓여낸 분식 메뉴"),
            new MenuItemData(7L, "면류", "신라면", 3900, "얼큰한 국물로 부담 없이 즐기는 기본 라면"),
            new MenuItemData(8L, "면류", "진라면", 3900, "순한 맛과 매운 맛의 균형이 좋은 대중적인 라면"),
            new MenuItemData(9L, "면류", "참깨라면", 4200, "참깨 풍미와 계란 블록이 살아 있는 고소한 라면"),
            new MenuItemData(10L, "간식 및 튀김류", "소떡소떡", 3800, "소시지와 떡을 달콤한 소스로 가볍게 즐기는 간식"),
            new MenuItemData(11L, "간식 및 튀김류", "치킨 가라아게", 5500, "겉은 바삭하고 속은 촉촉한 한입 치킨"),
            new MenuItemData(12L, "간식 및 튀김류", "감자튀김", 3500, "짭짤하고 바삭한 기본 사이드 메뉴"),
            new MenuItemData(13L, "간식 및 튀김류", "만두", 4000, "노릇하게 구워 간단히 곁들이기 좋은 만두"),
            new MenuItemData(14L, "음료수", "콜라", 2000, "시원하게 마시기 좋은 탄산음료"),
            new MenuItemData(15L, "음료수", "사이다", 2000, "깔끔한 청량감이 특징인 탄산음료"),
            new MenuItemData(16L, "음료수", "아메리카노", 2800, "진한 향과 깔끔한 끝맛의 커피"),
            new MenuItemData(17L, "음료수", "아이스티", 2500, "달콤하고 산뜻하게 즐기는 차가운 음료"),
            new MenuItemData(18L, "기타", "단무지 추가", 0, "느끼함을 잡아주는 아삭한 단무지 추가"),
            new MenuItemData(19L, "기타", "김치 추가", 0, "식사와 잘 어울리는 매콤한 김치 추가"),
            new MenuItemData(20L, "기타", "공깃밥 추가", 1000, "식사 메뉴와 곁들일 수 있는 따뜻한 공깃밥")
    );

    public CustomerOrderPlaceholderFrame(PcSocketClient socketClient, Long seatId) {
        this.socketClient = socketClient;
        this.seatId = seatId;
        setTitle("먹거리 주문");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1260, 1320);
        setLocationRelativeTo(null);
        menuScrollPane = createMenuScrollPane();
        setContentPane(createContent());
        showCategory(CATEGORY_ALL);
        refreshCart();
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        panel.setBackground(new Color(245, 241, 234));

        panel.add(createHeaderPanel(), BorderLayout.NORTH);
        panel.add(createCenterPanel(), BorderLayout.CENTER);
        panel.add(createCartPanel(), BorderLayout.EAST);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        panel.add(createCategoryPanel(), BorderLayout.NORTH);
        panel.add(menuScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JButton backButton = new JButton("이전");
        backButton.setFont(new Font("Dialog", Font.BOLD, 16));
        backButton.addActionListener(event -> dispose());

        JLabel titleLabel = new JLabel("먹거리 주문");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 34));

        headerLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
        headerLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(backButton);
        leftPanel.add(Box.createHorizontalStrut(14));
        leftPanel.add(titleLabel);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(headerLabel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 212, 200), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        panel.setBackground(new Color(250, 247, 242));
        panel.setOpaque(true);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JLabel label = new JLabel("목록");
        label.setFont(new Font("Dialog", Font.BOLD, 22));
        panel.add(label, BorderLayout.NORTH);

        List<String> categories = List.of(CATEGORY_ALL, "식사류", "면류", "간식 및 튀김류", "음료수", "기타");
        for (String category : categories) {
            JButton button = new JButton(category);
            button.setPreferredSize(new Dimension(150, 72));
            button.setFocusPainted(false);
            button.setFont(new Font("Dialog", Font.BOLD, 17));
            button.addActionListener(event -> showCategory(category));
            buttonPanel.add(button);
        }
        panel.add(buttonPanel, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createMenuScrollPane() {
        menuGridPanel.setOpaque(false);
        menuGridPanel.setLayout(new BoxLayout(menuGridPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(menuGridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setPreferredSize(new Dimension(310, 0));
        panel.setBackground(new Color(250, 247, 242));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 208, 197), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel titleLabel = new JLabel("장바구니");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);

        cartItemsPanel.setOpaque(false);
        cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));

        JScrollPane cartScrollPane = new JScrollPane(cartItemsPanel);
        cartScrollPane.setBorder(BorderFactory.createEmptyBorder());
        cartScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(cartScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        cartTotalLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        cartTotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(cartTotalLabel);
        bottomPanel.add(Box.createVerticalStrut(12));

        JButton clearButton = new JButton("장바구니 비우기");
        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        clearButton.addActionListener(event -> clearCart());

        JButton orderButton = new JButton("주문하기");
        orderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        orderButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        orderButton.setFont(new Font("Dialog", Font.BOLD, 18));
        orderButton.addActionListener(event -> submitOrderPlaceholder());

        bottomPanel.add(clearButton);
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(orderButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void showCategory(String category) {
        menuGridPanel.removeAll();
        List<MenuItemData> filteredItems = CATEGORY_ALL.equals(category)
                ? menuItems
                : menuItems.stream().filter(item -> item.category().equals(category)).toList();

        headerLabel.setText(CATEGORY_ALL.equals(category)
                ? "전체 메뉴 " + filteredItems.size() + "개"
                : category + " " + filteredItems.size() + "개");

        for (int index = 0; index < filteredItems.size(); index += 2) {
            JPanel rowPanel = createMenuRowPanel();
            rowPanel.add(createMenuCard(filteredItems.get(index), category));
            if (index + 1 < filteredItems.size()) {
                rowPanel.add(createMenuCard(filteredItems.get(index + 1), category));
            } else {
                rowPanel.add(createEmptyCardSpacer(category));
            }
            menuGridPanel.add(rowPanel);
            menuGridPanel.add(Box.createVerticalStrut(18));
        }

        menuGridPanel.revalidate();
        menuGridPanel.repaint();
        SwingUtilities.invokeLater(() -> menuScrollPane.getVerticalScrollBar().setValue(0));
    }

    private JPanel createMenuRowPanel() {
        JPanel rowPanel = new JPanel(new GridLayout(1, 2, 18, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return rowPanel;
    }

    private Component createEmptyCardSpacer(String currentCategory) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(0, resolveCardHeight(currentCategory)));
        return spacer;
    }

    private JPanel createMenuCard(MenuItemData item, String currentCategory) {
        boolean allCategory = CATEGORY_ALL.equals(currentCategory);
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        int preferredHeight = resolveCardHeight(currentCategory);
        card.setPreferredSize(new Dimension(0, preferredHeight));
        card.setMinimumSize(new Dimension(0, preferredHeight));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));

        if (allCategory) {
            card.add(createAllCategoryImageWrapper(item), BorderLayout.WEST);
            card.add(createAllCategoryInfoPanel(item), BorderLayout.CENTER);
            card.add(createMenuActionPanel(item), BorderLayout.SOUTH);
            return card;
        }

        card.add(createOtherCategoryInfoPanel(item), BorderLayout.NORTH);
        card.add(createCenteredImagePanel(item), BorderLayout.CENTER);
        card.add(createOtherCategoryBottomPanel(item), BorderLayout.SOUTH);
        return card;
    }

    private int resolveCardHeight(String currentCategory) {
        return CATEGORY_ALL.equals(currentCategory) ? 185 : 360;
    }

    private JPanel createAllCategoryImageWrapper(MenuItemData item) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(createImagePlaceholder(100, 120, item), BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createCenteredImagePanel(MenuItemData item) {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setOpaque(false);
        panel.add(createImagePlaceholder(200, 145, item));
        return panel;
    }

    private JPanel createImagePlaceholder(int width, int height, MenuItemData item) {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setPreferredSize(new Dimension(width, height));
        imagePanel.setBackground(IMAGE_BG_COLOR);
        imagePanel.setBorder(BorderFactory.createDashedBorder(IMAGE_BORDER_COLOR, 2, 6, 4, false));

        JLabel imageLabel = createImageLabel(item, width, height);
        if (imageLabel != null) {
            imagePanel.add(imageLabel, BorderLayout.CENTER);
            return imagePanel;
        }

        JLabel label = new JLabel("이미지 준비중", SwingConstants.CENTER);
        label.setFont(new Font("Dialog", Font.PLAIN, 12));
        label.setForeground(new Color(120, 110, 95));
        imagePanel.add(label, BorderLayout.CENTER);
        return imagePanel;
    }

    private JPanel createAllCategoryInfoPanel(MenuItemData item) {
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(item.name());
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 18));

        JLabel priceLabel = new JLabel(formatPrice(item.price()));
        priceLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        priceLabel.setForeground(new Color(167, 73, 42));

        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(priceLabel, BorderLayout.EAST);

        JLabel categoryLabel = new JLabel(item.category());
        categoryLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(113, 107, 98));
        categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea descriptionArea = new JTextArea(item.description());
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        descriptionArea.setForeground(new Color(83, 77, 70));
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pendingLabel = new JLabel("이미지는 나중에 연결되고, 지금은 장바구니에 담을 수 있습니다.");
        pendingLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        pendingLabel.setForeground(new Color(130, 124, 116));
        pendingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(topPanel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(categoryLabel);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(descriptionArea);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(pendingLabel);
        return infoPanel;
    }

    private JPanel createOtherCategoryInfoPanel(MenuItemData item) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setPreferredSize(new Dimension(0, 26));

        JLabel nameLabel = new JLabel(item.name());
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 18));

        JLabel priceLabel = new JLabel(formatPrice(item.price()));
        priceLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        priceLabel.setForeground(new Color(167, 73, 42));

        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(priceLabel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createOtherCategoryBottomPanel(MenuItemData item) {
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setOpaque(false);
        bottomPanel.setPreferredSize(new Dimension(0, 64));

        JTextArea descriptionArea = new JTextArea(item.description());
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        descriptionArea.setForeground(new Color(83, 77, 70));
        descriptionArea.setRows(2);

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.add(descriptionArea, BorderLayout.CENTER);
        bottomBar.add(createMenuActionPanel(item), BorderLayout.EAST);

        bottomPanel.add(bottomBar, BorderLayout.CENTER);
        return bottomPanel;
    }

    private JPanel createMenuActionPanel(MenuItemData item) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.setOpaque(false);

        JButton addButton = new JButton("장바구니 담기");
        addButton.setPreferredSize(new Dimension(130, 32));
        addButton.setFont(new Font("Dialog", Font.BOLD, 13));
        addButton.addActionListener(event -> addToCart(item));
        actionPanel.add(addButton);
        return actionPanel;
    }

    private void addToCart(MenuItemData item) {
        cartItems.put(item, cartItems.getOrDefault(item, 0) + 1);
        refreshCart();
    }

    private void clearCart() {
        if (cartItems.isEmpty()) {
            return;
        }
        cartItems.clear();
        refreshCart();
    }

    private void submitOrderPlaceholder() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "장바구니에 상품을 먼저 담아 주세요.");
            return;
        }
        try {
            for (Map.Entry<MenuItemData, Integer> entry : cartItems.entrySet()) {
                socketClient.placeOrder(seatId, entry.getKey().productId(), entry.getValue());
            }
            cartItems.clear();
            refreshCart();
            JOptionPane.showMessageDialog(this, "주문이 완료되었습니다.");
        } catch (BusinessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "주문 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshCart() {
        cartItemsPanel.removeAll();

        if (cartItems.isEmpty()) {
            JLabel emptyLabel = new JLabel("담긴 상품이 없습니다.");
            emptyLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
            emptyLabel.setForeground(new Color(120, 112, 101));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cartItemsPanel.add(emptyLabel);
        } else {
            for (Map.Entry<MenuItemData, Integer> entry : cartItems.entrySet()) {
                cartItemsPanel.add(createCartItemPanel(entry.getKey(), entry.getValue()));
                cartItemsPanel.add(Box.createVerticalStrut(10));
            }
        }

        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
        cartTotalLabel.setText("총액: " + formatPrice(calculateTotal()));
    }

    private JComponent createCartItemPanel(MenuItemData item, int quantity) {
        JPanel itemPanel = new JPanel(new BorderLayout(8, 8));
        itemPanel.setOpaque(true);
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 217, 205), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        itemPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemPanel.setPreferredSize(new Dimension(0, 128));
        itemPanel.setMinimumSize(new Dimension(0, 128));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));

        JLabel nameLabel = new JLabel(item.name());
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 15));

        JLabel priceLabel = new JLabel(formatPrice(item.price()) + " x " + quantity);
        priceLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        priceLabel.setForeground(new Color(130, 118, 103));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(priceLabel);

        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        quantityPanel.setOpaque(false);
        JButton minusButton = new JButton("-");
        JButton plusButton = new JButton("+");
        JLabel quantityLabel = new JLabel(String.valueOf(quantity));
        quantityLabel.setFont(new Font("Dialog", Font.BOLD, 15));
        minusButton.setPreferredSize(new Dimension(48, 32));
        plusButton.setPreferredSize(new Dimension(48, 32));
        minusButton.addActionListener(event -> changeCartQuantity(item, -1));
        plusButton.addActionListener(event -> changeCartQuantity(item, 1));
        quantityPanel.add(minusButton);
        quantityPanel.add(quantityLabel);
        quantityPanel.add(plusButton);

        itemPanel.add(textPanel, BorderLayout.CENTER);
        itemPanel.add(quantityPanel, BorderLayout.EAST);
        return itemPanel;
    }

    private void changeCartQuantity(MenuItemData item, int delta) {
        int nextQuantity = cartItems.getOrDefault(item, 0) + delta;
        if (nextQuantity <= 0) {
            cartItems.remove(item);
        } else {
            cartItems.put(item, nextQuantity);
        }
        refreshCart();
    }

    private int calculateTotal() {
        return cartItems.entrySet().stream()
                .mapToInt(entry -> entry.getKey().price() * entry.getValue())
                .sum();
    }

    private String formatPrice(int price) {
        if (price == 0) {
            return "무료";
        }
        return NumberFormat.getNumberInstance(Locale.KOREA).format(price) + "원";
    }

    private JLabel createImageLabel(MenuItemData item, int width, int height) {
        if (MENU_IMAGE_DIR == null) {
            return null;
        }

        Path imagePath = resolveImagePath(item);
        if (!Files.exists(imagePath)) {
            return null;
        }
        String cacheKey = imagePath.toAbsolutePath() + "|" + width + "x" + height;
        ImageIcon cachedIcon = scaledImageCache.get(cacheKey);
        if (cachedIcon != null) {
            return new JLabel(cachedIcon, SwingConstants.CENTER);
        }

        try {
            BufferedImage originalImage = ImageIO.read(imagePath.toFile());
            if (originalImage == null) {
                return null;
            }
            Image scaledImage = originalImage.getScaledInstance(width - 8, height - 8, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            scaledImageCache.put(cacheKey, scaledIcon);
            return new JLabel(scaledIcon, SwingConstants.CENTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Path resolveImagePath(MenuItemData item) {
        Path directPath = MENU_IMAGE_DIR.resolve(item.imageFileName());
        if (Files.exists(directPath)) {
            return directPath;
        }

        Path itemDirectory = MENU_IMAGE_DIR.resolve(String.valueOf(item.productId()));
        Path namedPath = itemDirectory.resolve("image.png");
        if (Files.exists(namedPath)) {
            return namedPath;
        }

        if (Files.isDirectory(itemDirectory)) {
            try (Stream<Path> files = Files.list(itemDirectory)) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(file -> isSupportedImageFile(file.getFileName().toString()))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .findFirst()
                        .orElse(directPath);
            } catch (Exception ignored) {
                return directPath;
            }
        }

        return directPath;
    }

    private boolean isSupportedImageFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static Path resolveMenuImageDir() {
        Path workingDirResolved = findMenuImageDir(Paths.get("").toAbsolutePath());
        if (workingDirResolved != null) {
            return workingDirResolved;
        }

        try {
            Path codeSource = Paths.get(CustomerOrderPlaceholderFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path codeSourceResolved = findMenuImageDir(codeSource);
            if (codeSourceResolved != null) {
                return codeSourceResolved;
            }
        } catch (URISyntaxException ignored) {
        }

        return Path.of("assets", "menu-images");
    }

    private static Path findMenuImageDir(Path startPath) {
        Path current = Files.isDirectory(startPath) ? startPath : startPath.getParent();
        while (current != null) {
            Path candidate = current.resolve("assets").resolve("menu-images");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private record MenuItemData(Long productId, String category, String name, int price, String description) {
        private String imageFileName() {
            return productId + ".png";
        }
    }
}
