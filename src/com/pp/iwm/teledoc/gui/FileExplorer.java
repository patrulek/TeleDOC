package com.pp.iwm.teledoc.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import com.pp.iwm.teledoc.layouts.AppWindowLayout;
import com.pp.iwm.teledoc.network.User.FileTreeListener;
import com.pp.iwm.teledoc.objects.File;
import com.pp.iwm.teledoc.objects.FileTree;
import com.pp.iwm.teledoc.utils.Utils;
import com.pp.iwm.teledoc.windows.AppWindow;
import com.pp.iwm.teledoc.windows.Window;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class FileExplorer extends Pane implements FileTreeListener {
	
	// =========================================
	// FIELDS
	// =========================================

	private AppWindowLayout layout;
	private Pane header_pane;
	private ImageButton btn_back;
	private Label lbl_path;
	private Pane simple_pane;
	private ScrollPane scroll_pane;
	
	private FileTree file_tree;
	private FileCard selected_card;
	private FileCard hovered_card;
	private List<FileCard> files;
	
	private int max_cards_in_row;
	private double cards_gap;
	private double icon_size;
	
	// ==========================================
	// METHODS
	// ==========================================
	
	public FileExplorer(AppWindowLayout _layout) {
		max_cards_in_row = 0;
		cards_gap = 20.0;
		icon_size = 32.0;
		files = new ArrayList<>();
		layout = _layout;
		
		createLayout();
		recalcMaxCardsInRow();
	}
	
	private void createLayout() {
		setPrefSize(759.0, 548.0);
		
		header_pane = new Pane();
		header_pane.setStyle("-fx-background-color: rgb(45, 81, 90);");
		header_pane.setPrefSize(759.0, 24.0);
		header_pane.setLayoutY(42.0);
		
		simple_pane = new Pane();
		simple_pane.setStyle("-fx-background-color: rgb(30, 54, 60);");
		simple_pane.setPrefWidth(759.0);
		
		btn_back = new ImageButton(Utils.IMG_PARENT_FOLDER_SMALL, Utils.HINT_PARENT_FOLDER, Utils.ACT_PARENT_FOLDER);
		btn_back.customizeZoomAnimation(1.15, 1.0, 200, 200);
		btn_back.disableFadeAnimation();
		btn_back.addEventHandler(MouseEvent.MOUSE_ENTERED, ev -> onBtnMouseEntered(btn_back));
		btn_back.addEventHandler(MouseEvent.MOUSE_EXITED, ev -> onBtnMouseExited(btn_back));
		btn_back.addEventHandler(ActionEvent.ACTION, ev -> onBtnMouseClicked(btn_back));
		
		lbl_path = new Label("root/");
		lbl_path.setFont(Utils.LBL_STATUSBAR_FONT);
		lbl_path.setStyle("-fx-text-fill: rgb(180, 180, 240); -fx-font-weight: bold;");
		lbl_path.setMaxWidth(600.0); lbl_path.setPrefHeight(24.0);
		lbl_path.setLayoutX(42.0);
		
		header_pane.getChildren().add(btn_back);
		header_pane.getChildren().add(lbl_path);
		
		scroll_pane = new ScrollPane(simple_pane);
		scroll_pane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scroll_pane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scroll_pane.setLayoutY(66.0);
		scroll_pane.setPrefSize(759.0, 469.0);
		scroll_pane.getStylesheets().add("/styles/file_pane.css");
		scroll_pane.setOnMouseClicked(event -> onScrollPaneMouseClicked());
		
		getChildren().add(header_pane);
		getChildren().add(scroll_pane);
	}

	// TODO hardcoded numbers
	private void recalcMaxCardsInRow() {
		max_cards_in_row = (int)(719.0 / (icon_size + cards_gap));
	}
	
	// TODO do optymalizacji
	public void refreshView() {
		clearView();
		refreshCurrentFolder();
		sortFiles();
		relocateFilesInExplorer();
	}
	
	private void refreshCurrentFolder() {
		for( Entry<String, File> entry : file_tree.getFilesForCurrentFolder().entrySet() ) {
			FileCard fc1 = new FileCard(this, entry.getValue());
			files.add(fc1);
		}
	}
	
	private void clearView() {
		files.clear();
		simple_pane.getChildren().clear();
	}
	
	private void sortFiles() {
		java.util.Collections.sort(files, new Comparator<FileCard>() {
			@Override
			public int compare(FileCard fc1, FileCard fc2) {
				if( fc1.getFile().isFolder() == fc2.getFile().isFolder() ) 
					return fc1.getFile().getName().compareTo(fc2.getFile().getName());
				else {
					if( fc1.getFile().isFolder() && !fc2.getFile().isFolder() )
						return -1;
					else if( !fc1.getFile().isFolder() && fc2.getFile().isFolder() )
						return 1;
					return 0;
				}
			}
		});
	}
	
	private void relocateFilesInExplorer() {
		for( int i = 0; i < files.size(); i++ ) {
			double x = (i % max_cards_in_row) * (icon_size + cards_gap) + 40.0 ;
			double y = (i / max_cards_in_row) * (icon_size + cards_gap * 2);
			
			simple_pane.getChildren().add(files.get(i));
			files.get(i).setLayoutX(x); files.get(i).setLayoutY(y);
		}
	}
	
	public void onCardSelect(FileCard _selected_card) {
		if( selected_card != null ) 
			selected_card.setNormalStyle();
		
		selected_card = _selected_card;
		selected_card.setSelectionStyle();
	}
	
	public void onCardChoose(FileCard _choosed_card) {
		File choosed_file = _choosed_card.getFile();
		
		if( choosed_file.isFolder() ) {
			file_tree.goIntoFolder(choosed_file);
			updateLabelPath();
			refreshView();
		} else
			; // TODO wy�wietl plik
	}
	
	public void onCardHover(FileCard _hovered_card) {
		hovered_card = _hovered_card;
	}

	public void addTextToStatusBar(String _text) {
		layout.addTextToStatusBar(_text);
	}
	
	public void removeTextFromStatusBar() {
		layout.removeTextFromStatusBar();
	}
	
	private void onScrollPaneMouseClicked() {
		if( selected_card != null && hovered_card == null) {
			selected_card.setNormalStyle();
			selected_card = null;
		}
	}
	
	private void onBtnMouseClicked(ImageButton _ibtn) {
		if( _ibtn == btn_back ) {
			if( file_tree.hasCurrentFolderAParent() ) {
				if( selected_card != null ) {
					selected_card = null;
					removeTextFromStatusBar();
				}
				
				file_tree.goParentFolderIfExist();
				updateLabelPath();
				refreshView();
			}
		}
	}
	
	private void updateLabelPath() {
		lbl_path.setText(file_tree.getCurrentFolder().getPath());
	}
	
	private void onBtnMouseEntered(ImageButton _ibtn) {
		addTextToStatusBar(_ibtn.getHint());
	}
	
	private void onBtnMouseExited(ImageButton _ibtn) {
		removeTextFromStatusBar();
	}
	
	public void setFileTree(FileTree _file_tree) {
		file_tree = _file_tree;
	}
	
	public FileCard getSelectedCard() {
		return selected_card;
	}

	@Override
	public void onFileTreeChanged(FileTree _file_tree) {
		file_tree = _file_tree;
		Platform.runLater(() -> refreshView());
	}
	
}
