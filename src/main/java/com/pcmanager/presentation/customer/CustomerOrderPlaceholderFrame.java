package com.pcmanager.presentation.customer;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.infrastructure.network.PcSocketClient;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 고객이 먹거리 주문을 진행하는 메인 프레임이다.
 *
 * 이 프레임은 아래 4가지 역할을 한 곳에서 처리한다.
 * 1. 카테고리별 메뉴 목록 렌더링
 * 2. 메뉴 이미지 로딩 및 캐시
 * 3. 장바구니 관리
 * 4. 서버로 실제 주문 전송
 *
 * 최근 기능이 많이 붙으면서 메서드가 섞이기 쉬워서, 아래처럼 영역을 나눠 정리했다.
 * - 상단/레이아웃 생성
 * - 메뉴 목록 렌더링
 * - 장바구니 렌더링
 * - 주문 실행
 * - 이미지 로딩
 */
public class CustomerOrderPlaceholderFrame extends JFrame {
    private static final String CATEGORY_ALL = "전체";

    private static final int FRAME_WIDTH = 1260;
    private static final int FRAME_HEIGHT = 1320;
    private static final int CATEGORY_BUTTON_WIDTH = 150;
    private static final int CATEGORY_BUTTON_HEIGHT = 72;
    private static final int ALL_CATEGORY_CARD_HEIGHT = 185;
    private static final int DETAIL_CATEGORY_CARD_HEIGHT = 360;
    private static final int DETAIL_IMAGE_WIDTH = 200;
    private static final int DETAIL_IMAGE_HEIGHT = 145;
    private static final int ALL_IMAGE_WIDTH = 100;
    private static final int ALL_IMAGE_HEIGHT = 120;

    private static final Color FRAME_BACKGROUND = new Color(245, 241, 234);
    private static final Color PANEL_BACKGROUND = new Color(250, 247, 242);
    private static final Color CARD_BORDER_COLOR = new Color(215, 205, 190);
    private static final Color CART_BORDER_COLOR = new Color(216, 208, 197);
    private static final Color IMAGE_BG_COLOR = new Color(239, 234, 225);
    private static final Color IMAGE_BORDER_COLOR = new Color(180, 168, 150);
    private static final Color PRICE_COLOR = new Color(167, 73, 42);
    private static final Color SUB_TEXT_COLOR = new Color(130, 124, 116);
    private static final Color DESCRIPTION_COLOR = new Color(83, 77, 70);

    /**
     * 실행 위치가 IDE / shell / out/classes 어디든 최대한 프로젝트 루트의 이미지 폴더를 찾기 위한 기준 경로다.
     */
    private static final Path MENU_IMAGE_DIR = resolveMenuImageDir();

    private final PcSocketClient socketClient;
    private final Long seatId;

    /**
     * 카드 목록이 들어가는 컨테이너다.
     * 카테고리를 바꿀 때마다 이 패널을 비우고 다시 구성한다.
     */
    private final JPanel menuGridPanel = new JPanel();

    /**
     * 장바구니 목록이 들어가는 컨테이너다.
     * 장바구니 수량이 바뀔 때마다 이 패널을 다시 그린다.
     */
    private final JPanel cartItemsPanel = new JPanel();

    private final JLabel headerLabel = new JLabel();
    private final JLabel cartTotalLabel = new JLabel();
    private final JScrollPane menuScrollPane;

    /**
     * 장바구니는 메뉴 데이터와 수량을 1:1로 관리한다.
     * LinkedHashMap을 쓰는 이유는 사용자가 담은 순서를 그대로 장바구니에 보여주기 위해서다.
     */
    private final Map<MenuItemData, Integer> cartItems = new LinkedHashMap<>();

    /**
     * 이미지 파일을 디스크에서 반복 로드하면 주문창이 버벅거리기 쉽다.
     * 한 번 축소한 이미지는 캐시에 저장해 다시 열거나 카테고리를 바꿀 때 재사용한다.
     */
    private final Map<String, ImageIcon> scaledImageCache = new LinkedHashMap<>();

    /**
     * 카테고리 이름으로 메뉴 목록을 찾기 위한 캐시다.
     * "전체"는 이 맵에 넣지 않고, renderCategory에서 map의 모든 값을 합쳐 계산한다.
     */
    private final Map<String, List<MenuItemData>> menuItemsByCategory = new LinkedHashMap<>();

    /**
     * 현재 주문창에서 사용하는 메뉴 정의다.
     * productId는 서버 bootstrap 상품 ID와 반드시 맞아야 실제 주문이 정상 저장된다.
     */
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
        this.menuScrollPane = createMenuScrollPane();
        initializeCategoryMap();

        setTitle("먹거리 주문");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setContentPane(createContent());

        // 처음 열릴 때 카테고리 전환 렉을 줄이기 위해 모든 메뉴 이미지를 대표 크기 기준으로 미리 캐시한다.
        preloadMenuImageCache();
        renderCategory(CATEGORY_ALL);
        refreshCart();
    }

    /**
     * 카테고리 맵을 한 번만 만들어 이후 필터링 시 반복 분류 비용이 들지 않게 한다.
     */
    private void initializeCategoryMap() {
        for (MenuItemData item : menuItems) {
            menuItemsByCategory.computeIfAbsent(item.category(), key -> new java.util.ArrayList<>()).add(item);
        }
    }

    /**
     * 주문창 첫 진입 시 자주 쓰는 두 가지 대표 이미지 크기를 미리 캐시에 넣는다.
     */
    private void preloadMenuImageCache() {
        for (List<MenuItemData> items : menuItemsByCategory.values()) {
            for (MenuItemData item : items) {
                createImageLabel(item, ALL_IMAGE_WIDTH, ALL_IMAGE_HEIGHT);
                createImageLabel(item, DETAIL_IMAGE_WIDTH, DETAIL_IMAGE_HEIGHT);
            }
        }
    }

    /**
     * 프레임 전체 레이아웃을 조립한다.
     * - 북쪽: 이전 버튼 + 타이틀 + 현재 카테고리 헤더
     * - 가운데: 카테고리 버튼 + 메뉴 카드 목록
     * - 오른쪽: 장바구니
     */
    private JPanel createContent() {
        JPanel rootPanel = new JPanel(new BorderLayout(18, 18));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        rootPanel.setBackground(FRAME_BACKGROUND);

        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        rootPanel.add(createMenuAreaPanel(), BorderLayout.CENTER);
        rootPanel.add(createCartPanel(), BorderLayout.EAST);
        return rootPanel;
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

    private JPanel createMenuAreaPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setOpaque(false);
        panel.add(createCategoryPanel(), BorderLayout.NORTH);
        panel.add(menuScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 212, 200), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JLabel label = new JLabel("목록");
        label.setFont(new Font("Dialog", Font.BOLD, 22));
        panel.add(label, BorderLayout.NORTH);

        // 카테고리가 6개라 가로 한 줄 배치에서는 마지막 버튼이 잘릴 수 있어
        // 2행 3열 고정 배치로 바꿔 항상 모든 버튼이 보이게 한다.
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        buttonPanel.setOpaque(false);

        buttonPanel.add(createCategoryButton(CATEGORY_ALL));
        for (String category : menuItemsByCategory.keySet()) {
            buttonPanel.add(createCategoryButton(category));
        }

        panel.add(buttonPanel, BorderLayout.CENTER);
        return panel;
    }

    private JButton createCategoryButton(String category) {
        JButton button = new JButton(category);
        button.setPreferredSize(new Dimension(CATEGORY_BUTTON_WIDTH, CATEGORY_BUTTON_HEIGHT));
        button.setFocusPainted(false);
        button.setFont(new Font("Dialog", Font.BOLD, 17));
        button.addActionListener(event -> renderCategory(category));
        return button;
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

    /**
     * 카테고리 변경 시 호출된다.
     *
     * 기존 카드를 모두 비우고, 2개씩 한 줄로 묶어서 다시 렌더링한다.
     * GridLayout 전체를 직접 쓰면 카테고리별 개수에 따라 카드 높이가 흔들리기 쉬워서
     * 행 단위 패널을 쌓는 구조로 고정했다.
     */
    private void renderCategory(String category) {
        menuGridPanel.removeAll();

        List<MenuItemData> filteredItems = resolveCategoryItems(category);

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

        // 카테고리를 바꿨을 때 항상 첫 번째 상품부터 보이도록 스크롤을 맨 위로 올린다.
        SwingUtilities.invokeLater(() -> menuScrollPane.getVerticalScrollBar().setValue(0));
    }

    private List<MenuItemData> resolveCategoryItems(String category) {
        if (CATEGORY_ALL.equals(category)) {
            return menuItemsByCategory.values().stream()
                    .flatMap(List::stream)
                    .toList();
        }
        return menuItemsByCategory.getOrDefault(category, List.of());
    }

    private JPanel createMenuRowPanel() {
        JPanel rowPanel = new JPanel(new GridLayout(1, 2, 18, 0));
        rowPanel.setOpaque(false);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return rowPanel;
    }

    private Component createEmptyCardSpacer(String category) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(0, resolveCardHeight(category)));
        return spacer;
    }

    /**
     * 전체 카테고리와 상세 카테고리는 레이아웃이 다르다.
     * - 전체: 좌측 이미지, 우측 정보
     * - 비전체: 상단 제목/가격, 중앙 이미지, 하단 설명
     */
    private JPanel createMenuCard(MenuItemData item, String category) {
        boolean isAllCategory = CATEGORY_ALL.equals(category);
        JPanel card = createCardShell(resolveCardHeight(category));

        if (isAllCategory) {
            card.add(createSideImagePanel(item), BorderLayout.WEST);
            card.add(createAllCategoryInfoPanel(item), BorderLayout.CENTER);
            card.add(createMenuActionPanel(item), BorderLayout.SOUTH);
            return card;
        }

        card.add(createOtherCategoryHeaderPanel(item), BorderLayout.NORTH);
        card.add(createCenteredImagePanel(item), BorderLayout.CENTER);
        card.add(createOtherCategoryBottomPanel(item), BorderLayout.SOUTH);
        return card;
    }

    private JPanel createCardShell(int preferredHeight) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        card.setPreferredSize(new Dimension(0, preferredHeight));
        card.setMinimumSize(new Dimension(0, preferredHeight));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));
        return card;
    }

    private int resolveCardHeight(String category) {
        return CATEGORY_ALL.equals(category) ? ALL_CATEGORY_CARD_HEIGHT : DETAIL_CATEGORY_CARD_HEIGHT;
    }

    private JPanel createSideImagePanel(MenuItemData item) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(createImagePlaceholder(ALL_IMAGE_WIDTH, ALL_IMAGE_HEIGHT, item), BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createCenteredImagePanel(MenuItemData item) {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setOpaque(false);
        panel.add(createImagePlaceholder(DETAIL_IMAGE_WIDTH, DETAIL_IMAGE_HEIGHT, item));
        return panel;
    }

    /**
     * 이미지가 있으면 실제 이미지를 렌더링하고,
     * 없으면 동일한 크기의 플레이스홀더를 보여준다.
     */
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

        JLabel fallbackLabel = new JLabel("이미지 준비중", SwingConstants.CENTER);
        fallbackLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        fallbackLabel.setForeground(new Color(120, 110, 95));
        imagePanel.add(fallbackLabel, BorderLayout.CENTER);
        return imagePanel;
    }

    private JPanel createAllCategoryInfoPanel(MenuItemData item) {
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        infoPanel.add(createMenuTitleRow(item, 18, 16));
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(createSecondaryLabel(item.category()));
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(createReadOnlyDescription(item.description(), 0));
        return infoPanel;
    }

    private JPanel createOtherCategoryHeaderPanel(MenuItemData item) {
        JPanel topPanel = createMenuTitleRow(item, 18, 16);
        topPanel.setPreferredSize(new Dimension(0, 26));
        return topPanel;
    }

    private JPanel createOtherCategoryBottomPanel(MenuItemData item) {
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setOpaque(false);
        bottomPanel.setPreferredSize(new Dimension(0, 64));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(createReadOnlyDescription(item.description(), 2), BorderLayout.CENTER);
        contentPanel.add(createMenuActionPanel(item), BorderLayout.EAST);

        bottomPanel.add(contentPanel, BorderLayout.CENTER);
        return bottomPanel;
    }

    private JPanel createMenuTitleRow(MenuItemData item, int nameFontSize, int priceFontSize) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(item.name());
        nameLabel.setFont(new Font("Dialog", Font.BOLD, nameFontSize));

        JLabel priceLabel = new JLabel(formatPrice(item.price()));
        priceLabel.setFont(new Font("Dialog", Font.BOLD, priceFontSize));
        priceLabel.setForeground(PRICE_COLOR);

        topPanel.add(nameLabel, BorderLayout.WEST);
        topPanel.add(priceLabel, BorderLayout.EAST);
        return topPanel;
    }

    private JLabel createSecondaryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.PLAIN, 12));
        label.setForeground(new Color(113, 107, 98));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea createReadOnlyDescription(String text, int rows) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        textArea.setForeground(DESCRIPTION_COLOR);
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (rows > 0) {
            textArea.setRows(rows);
        }
        return textArea;
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

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setPreferredSize(new Dimension(310, 0));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CART_BORDER_COLOR, 1),
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
        orderButton.addActionListener(event -> submitOrders());

        bottomPanel.add(clearButton);
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(orderButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void addToCart(MenuItemData item) {
        cartItems.put(item, cartItems.getOrDefault(item, 0) + 1);
        refreshCart();
    }

    /**
     * 장바구니 전체를 비우고 우측 패널을 즉시 다시 그린다.
     */
    private void clearCart() {
        if (cartItems.isEmpty()) {
            return;
        }
        cartItems.clear();
        refreshCart();
    }

    /**
     * 장바구니 각 항목을 서버 주문으로 전송한다.
     *
     * 현재 서버 프로토콜은 "한 번에 한 상품" 주문 구조라서,
     * 장바구니를 순회하면서 상품별 주문을 연속 생성한다.
     */
    private void submitOrders() {
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

    /**
     * 현재 장바구니 상태를 우측 패널에 다시 렌더링한다.
     * 항목이 없으면 빈 상태 문구를, 있으면 각 상품 패널과 총액을 보여준다.
     */
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
        cartTotalLabel.setText("총액: " + formatPrice(calculateTotalPrice()));
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
        minusButton.setPreferredSize(new Dimension(48, 32));
        minusButton.addActionListener(event -> changeCartQuantity(item, -1));

        JButton plusButton = new JButton("+");
        plusButton.setPreferredSize(new Dimension(48, 32));
        plusButton.addActionListener(event -> changeCartQuantity(item, 1));

        JLabel quantityLabel = new JLabel(String.valueOf(quantity));
        quantityLabel.setFont(new Font("Dialog", Font.BOLD, 15));

        quantityPanel.add(minusButton);
        quantityPanel.add(quantityLabel);
        quantityPanel.add(plusButton);

        itemPanel.add(textPanel, BorderLayout.CENTER);
        itemPanel.add(quantityPanel, BorderLayout.EAST);
        return itemPanel;
    }

    /**
     * 장바구니 수량 증감 처리다.
     * 0 이하가 되면 해당 항목을 제거한다.
     */
    private void changeCartQuantity(MenuItemData item, int delta) {
        int nextQuantity = cartItems.getOrDefault(item, 0) + delta;
        if (nextQuantity <= 0) {
            cartItems.remove(item);
        } else {
            cartItems.put(item, nextQuantity);
        }
        refreshCart();
    }

    /**
     * 장바구니 총액을 실시간 계산한다.
     */
    private int calculateTotalPrice() {
        return cartItems.entrySet().stream()
                .mapToInt(entry -> entry.getKey().price() * entry.getValue())
                .sum();
    }

    /**
     * 0원 상품은 "무료", 그 외는 한국식 천 단위 구분 문자열로 표시한다.
     */
    private String formatPrice(int price) {
        if (price == 0) {
            return "무료";
        }
        return NumberFormat.getNumberInstance(Locale.KOREA).format(price) + "원";
    }

    /**
     * 이미지 로딩 흐름
     * 1. 상품 ID 기준으로 실제 파일 경로를 찾는다.
     * 2. 찾은 파일을 지정 크기로 축소한다.
     * 3. 축소 결과를 캐시에 넣고 재사용한다.
     *
     * 주문창 렉이 심했던 원인이 반복 디스크 읽기 + 반복 리사이즈라서
     * 이 메서드는 성능 관점에서 가장 중요한 지점이다.
     */
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

    /**
     * 이미지 탐색 우선순위
     * 1. assets/menu-images/1.png
     * 2. assets/menu-images/1/image.png
     * 3. assets/menu-images/1 폴더 안의 첫 번째 png/jpg/jpeg
     *
     * 사용자가 파일명을 제각각 넣더라도 "상품 ID 폴더 안에 이미지가 있으면" 최대한 찾도록 설계했다.
     */
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

    /**
     * 현재는 png/jpg/jpeg만 메뉴 이미지로 허용한다.
     */
    private boolean isSupportedImageFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    /**
     * 프로젝트를 IDE에서 실행하든, shell에서 실행하든, out/classes 기준으로 실행하든
     * assets/menu-images 폴더를 찾을 수 있게 시작 경로를 여러 방식으로 계산한다.
     */
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

    /**
     * 시작 경로에서 부모 방향으로 올라가며 assets/menu-images 폴더를 찾는다.
     */
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

    /**
     * 화면 렌더링에 필요한 최소 메뉴 정보만 묶은 값 객체다.
     * 서버 상품 ID와 UI 표시 정보가 같이 있어야 주문 전송과 렌더링을 동시에 처리할 수 있다.
     */
    private record MenuItemData(Long productId, String category, String name, int price, String description) {
        private String imageFileName() {
            return productId + ".png";
        }
    }
}
