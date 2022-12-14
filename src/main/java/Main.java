import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import javax.swing.*;
import javax.swing.text.DefaultCaret;


public class Main extends JFrame {
    final String VERSION = "1.0";
    String textPath = "";
    String versionURL = "https://pinapelz.github.io/ytmp3AutoTag/version.txt";

    String formats[] = {"maxresdefault.jpg","mqdefault.jpg","hqdefault.jpg"};

    JPanel panel = new JPanel();
    JScrollPane scrollPane;
    JButton songsGen = new JButton("Generate text file");
    JButton editButton = new JButton("Edit Tags");
    JButton startButton = new JButton("Set .txt File");
    JCheckBox defaultFileBox = new JCheckBox("Use Default songs.txt file");
    JCheckBox useBlacklistBox = new JCheckBox("Use Blacklist.txt");
    JProgressBar progressBar = new JProgressBar();
    static JTextArea outputArea = new JTextArea("");
    JLabel title = new JLabel("YouTube to MP3 Auto Tagging [1]");
    JLabel version = new JLabel("Version: " + VERSION);

    int progress = 0;
    Boolean useBlacklist = false;
    Boolean readyState = false;
    Boolean useDefault = false;
    FileUtility fileUtil = new FileUtility();

    public Main(){
        initializeComponents();
        initializeActionsListeners();
    }

    public static void main(String[] args) {
        new Main().setVisible(true);
    }

    private void downloadAndTag(){
        ArrayList<String> songs = fileUtil.txtToArrayList(textPath);
        progress = 0;
        String timeAppend = "";
        boolean partFlag = false;
        for(int i = 0;i<songs.size();i++) {
            try {
                fileUtil.deleteAllFilesDir("downloaded");
                ArrayList<String> splitStamp = null;
                try{
                    splitStamp = new ArrayList<>(Arrays.asList(songs.get(i).split(",")));
                }
                catch(Exception e) {

                }
                if(splitStamp.size()>=2){
                    timeAppend = youtubeToMP3Part(splitStamp.get(0),splitStamp.get(1));
                    partFlag = true;
                }
                else{
                    youtubeToMP3Full(songs.get(i));
                }

                String info[] = fileUtil.parseJson(fileUtil.jsonToString(fileUtil.findJsonFile("downloaded"))); //title,uploader
                String uploader = info[1];
                String title = info[0];
                if(useBlacklist){
                    System.out.println("Using blacklist");
                    uploader = fileUtil.removeBlacklist(uploader,"blacklist.txt");
                    title = fileUtil.removeBlacklist(title,"blacklist.txt");
                }
                AudioFile f = AudioFileIO.read(fileUtil.findMP3File("downloaded"));
                Tag tag = f.getTag();
                System.out.println("Uploader: "+uploader);
                System.out.println("Title: "+title);
                tag.setField(FieldKey.ARTIST, uploader);
                tag.setField(FieldKey.TITLE, title);
                fileUtil.downloadImage("https://img.youtube.com/vi/"+info[2]+"/","img.jpg",formats);
                Artwork cover = Artwork.createArtworkFromFile(new File("img.jpg"));
                tag.addField(cover);
                f.commit();
                fileUtil.deleteFile("img.jpg");
                if(partFlag){
                    fileUtil.moveFile(fileUtil.findMP3File("downloaded").getAbsolutePath(), "completed/"
                            + fileUtil.removeNonAlphaNumeric(info[0]) +
                            " ["+info[2]+ "]"+timeAppend+".mp3");
                }
                else{
                    fileUtil.moveFile(fileUtil.findMP3File("downloaded").getAbsolutePath(), "completed/" +
                            fileUtil.removeNonAlphaNumeric(info[0]) + " ["+info[2]+ "].mp3");
                }
                outputArea.setText(outputArea.getText()+"\n"+"Moved file to Completed Folder");
                progress = i;
                System.out.println("Current Progress " + calculatePercentage(i+1,songs.size()));
                progressBar.setValue(calculatePercentage(i+1,songs.size()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int calculatePercentage(int current, int total){//Calculate the percentage when give numerator and denominator
        double currentD = current;
        double totalD = total;
        return (int)((currentD/totalD)*100);
    }

    public static void showWarning(String message) {
        JOptionPane.showMessageDialog(null, message, "JUST YOUR FRIENDLY NEIGHBORLY REMINDER", JOptionPane.WARNING_MESSAGE);
    }

    public static void youtubeToMP3Full(String url) {//Download mp3 of youtube video using yt-dlp.exe. Ran from cmd
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "--output", "downloaded/%(title)s_%(id)s.mp3",
                    "--ffmpeg-location","ffmpeg.exe",
                    "--write-info-json",
                    url
            );
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                outputArea.setText(outputArea.getText()+"\n"+line);
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static String youtubeToMP3Part(String url,String stamp) {//Download mp3 of youtube video using yt-dlp.exe. Ran from cmd
        System.out.println(url + " " + stamp);
        ArrayList<String> times = new ArrayList<>(Arrays.asList(stamp.split("-")));
        ArrayList<String> startTimeComponents = new ArrayList<>(Arrays.asList(times.get(0).split(":")));
        ArrayList<String> endTimeComponents = new ArrayList<>(Arrays.asList(times.get(1).split(":")));
        int startSec = 0;
        int endSec = 0;
        if(startTimeComponents.size()==3){
            startSec = Integer.parseInt(startTimeComponents.get(0))*60*60+Integer.parseInt(startTimeComponents.get(1))*60+Integer.parseInt(startTimeComponents.get(2));
        }
        else if(startTimeComponents.size()==2){
            startSec = Integer.parseInt(startTimeComponents.get(0))*60+Integer.parseInt(startTimeComponents.get(1));
        }
        if(endTimeComponents.size()==3){
            endSec = Integer.parseInt(endTimeComponents.get(0))*60*60+Integer.parseInt(endTimeComponents.get(1))*60+Integer.parseInt(endTimeComponents.get(2));
        }
        else if(endTimeComponents.size()==2){
            endSec = Integer.parseInt(endTimeComponents.get(0))*60+Integer.parseInt(endTimeComponents.get(1));
        }
       try {
            ProcessBuilder builder = new ProcessBuilder(
                    "yt-dlp.exe",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "--output", "downloaded/%(title)s_%(id)s.mp3",
                    "--ffmpeg-location","ffmpeg.exe",
                    "--write-info-json","--download-sections","\"*"+startSec+"-"+endSec+"\"",
                    "--force-keyframes-at-cuts",
                    url
            );
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                outputArea.setText(outputArea.getText()+"\n"+line);
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
       return startSec+"to"+endSec;
    }


    private void initializeComponents(){//Initiate GUI components
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.add(panel);
        panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputArea.setEditable(true);
        outputArea.setLineWrap(true);
        DefaultCaret caret = (DefaultCaret)outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        panel.add(Box.createRigidArea(new Dimension(0,5)));
        panel.setBorder(BorderFactory.createEmptyBorder(25,10,20,10));
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        defaultFileBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        useBlacklistBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        editButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        songsGen.setAlignmentX(Component.CENTER_ALIGNMENT);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setStringPainted(true);
        title.setFont(new Font("Verdana", Font.PLAIN, 14));
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(10));
        panel.add(startButton);
        panel.add(defaultFileBox);
        panel.add(Box.createVerticalStrut(8));
        panel.add(scrollPane);
        panel.add(Box.createVerticalStrut(5));
        panel.add(editButton);
        panel.add(useBlacklistBox);
        panel.add(Box.createVerticalStrut(8));
        panel.add(version);
        //panel.add(Box.createVerticalStrut(5));
        //panel.add(songsGen);

        this.setSize(550,450);
        this.setTitle("YTMP3Tagger");
        checkUpdate();

    }

    private void initializeActionsListeners(){//Add all actionlisteners for buttons
        defaultFileBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File f = new File("songs.txt");
                if(f.exists()&!f.isDirectory()&&!useDefault) {
                    System.out.println("songs found");
                    textPath = "songs.txt";
                    showWarning("Default File has been set.\nMake sure you add a new line for each URL");
                    readyState = true;
                    startButton.setText("Start Download");
                    outputArea.setText(outputArea.getText() + "\n" + "Ready to begin downloading. Press the button");
                    System.out.println("Ready to begin downloading. Press the button");
                    useDefault = true;

                }
                else{
                    useDefault = false;
                    readyState = false;
                    startButton.setText("Set .txt file");
                }
            }
        });
        useBlacklistBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if(useBlacklistBox.isSelected()){
                    useBlacklist = true;
                }
                else{
                    useBlacklist = false;
                }

            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (readyState==false){
                    outputArea.setText(outputArea.getText()+"\n"+"txt path has not been set. Launching chooserPane");
                    System.out.println(".txt path has not been set. Launching chooserPane");
                    textPath = fileUtil.showTextFileChooser();
                    try {
                        if (!textPath.equals("")) {
                            showWarning("File has been set.\nMake sure you add a new line for each URL");
                            readyState = true;
                            startButton.setText("Start Download");
                            outputArea.setText(outputArea.getText() + "\n" + "Ready to begin downloading. Press the button");
                            System.out.println("Ready to begin downloading. Press the button");
                        }
                    }
                    catch(Exception ex){

                    }
                }
                else{
                    Runnable runnable = () -> {
                        outputArea.setText("");
                        startButton.setEnabled(false);
                        downloadAndTag();
                        startButton.setEnabled(true);

                    };
                    Thread thread = new Thread(runnable);
                    thread.start();
                }
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                new TagEditorScreen().setVisible(true);
            }
        });
        songsGen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
            }
        });
    }
    private void checkUpdate(){ //Check website for any updates and if the versions do not match then download new upate
        try{
            URL url = new URL(versionURL);
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String body = IOUtils.toString(in, encoding);
            ArrayList<String> lines = new ArrayList<String>(Arrays.asList(body.split("\\r?\\n")));
            if(VERSION.equals(lines.get(0))){
                System.out.println("Program is up to date");
            }
            else{
                int dialogResult = JOptionPane.showConfirmDialog (null, "New Version Found!\nWould you like to download the new version?","New Version Found!",JOptionPane.YES_NO_OPTION);
                if(dialogResult==JOptionPane.YES_OPTION){
                lines.remove(0);
                for(int i =0;i<lines.size();i++) {
                    if (lines.get(i).startsWith("add")) {
                        lines.set(i, lines.get(i).replace("add", ""));
                        String[] parts = lines.get(i).split("/");
                        ProcessBuilder builder = new ProcessBuilder(
                                "cmd.exe", "/c", "curl " + lines.get(i) + " -o " + parts[parts.length - 1]);
                        builder.redirectErrorStream(true);
                        Process p = builder.start();
                        p.waitFor();
                    }
                    //delete old files
                    else if (lines.get(i).startsWith("del")) {
                        lines.set(i, lines.get(i).replace("del", ""));
                        lines.set(i, lines.get(i).replaceAll("\\s+", ""));
                        System.out.println(lines.get(i));
                        File f = new File(lines.get(i));
                        f.delete();
                    }
                    //when the new version update process calls for end the program will end and relaunch the new version
                    else if (lines.get(i).equals("end")) {
                        ProcessBuilder builder = new ProcessBuilder(
                                "cmd.exe", "/c", "RUN_THIS_TO_START_PROGRAM.bat");
                        Process p = builder.start();
                        p.waitFor();
                        System.exit(0);
                    }
                }
                }
                else{

                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }


}
