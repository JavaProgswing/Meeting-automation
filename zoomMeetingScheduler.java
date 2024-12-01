import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

//Zoom automation tool
public class zoomMeetingScheduler {
    boolean meetingEnded = false;
    private String meetingId;
    private String url;
    private String name = "Zoom meeting";

    public static boolean isafterTime(Date e) {
        Date now = new Date();
        return now.after(e);
    }

    public static boolean isbeforeTime(Date e) {
        Date now = new Date();
        return now.before(e);
    }

    public static void main(String[] args) {
        try {
            if (args.length != 3 && args.length != 4 && args.length != 5) {
                throw new meetingDetailsInvalidException("Try zoomMeetingScheduler MEETINGID PASSWORD HH:MM:SS ");
            }
            String meetingId = args[0];
            String password = args[1];
            String schtime = args[2];
            String weekdays = "";
            zoomMeetingScheduler obj = new zoomMeetingScheduler();
            if (args.length >= 4)
                weekdays = args[3];
            if (args.length == 5)
                obj.name = args[4];
            obj.meetingId = args[0];
            System.out.println("Meeting id : " + meetingId);
            System.out.println("Meeting name : " + obj.name);
            System.out.println("Scheduled weekdays : " + weekdays);
            System.out.println("Time : " + schtime);
            System.out.println(" - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
            if (password.equals("GoogleMeet")) {
                if (schtime.trim().isEmpty()) {
                    String hour = obj.getCurrentHour(), min = obj.getCurrentMinute(), sec = "00";
                    min = Integer.toString(Integer.parseInt(min) + 1);
                    schtime = hour + ":" + min + ":" + sec;
                }
                Date DschTime = obj.dateFromHourMinSec(schtime);
                obj.waitUntil(DschTime, weekdays);
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(meetingId));
                System.out.print("\rSTATUS : JOINED");
                System.exit(0);
            }
            obj.url = String.format("zoommtg://zoom.us/join?confno=%s&pwd=%s", meetingId, password);
            if (schtime.trim().equals("")) {
                String hour = obj.getCurrentHour(), min = obj.getCurrentMinute(), sec = "00";
                min = Integer.toString(Integer.parseInt(min) + 1);
                schtime = hour + ":" + min + ":" + sec;
            }
            Date DschTime = obj.dateFromHourMinSec(schtime);
            obj.waitUntil(DschTime, weekdays);
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(obj.url));
            obj.waitPostMeeting();
        } catch (Exception e) {
            JOptionPane optionPane = new JOptionPane(e.getMessage(), JOptionPane.ERROR_MESSAGE);
            JDialog dialog = optionPane.createDialog("ZS failure");
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
        }
    }

    private String getCurrentHour() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("HH");  //it will give you the date in the formate that is given in the image
        String datetime = dateformat.format(c.getTime()); // it will give you the date
        return datetime;
    }

    private String getCurrentMinute() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("mm");  //it will give you the date in the formate that is given in the image
        String datetime = dateformat.format(c.getTime()); // it will give you the date
        return datetime;
    }

    /*private String getCurrentSecond() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateformat = new SimpleDateFormat("ss");  //it will give you the date in the formate that is given in the image
        String datetime = dateformat.format(c.getTime()); // it will give you the date
        return datetime;
    }*/
    private Date dateFromHourMinSec(final String hhmmss) {
        if (hhmmss.matches("^[0-2][0-9]:[0-5][0-9]:[0-5][0-9]$")) {
            final String[] hms = hhmmss.split(":");
            final GregorianCalendar gc = new GregorianCalendar();
            gc.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hms[0]));
            gc.set(Calendar.MINUTE, Integer.parseInt(hms[1]));
            gc.set(Calendar.SECOND, Integer.parseInt(hms[2]));
            gc.set(Calendar.MILLISECOND, 0);
            return gc.getTime();
        } else {
            throw new IllegalArgumentException(hhmmss + " is not a valid time, expecting HH:MM:SS format");
        }
    }

    void waitPostMeeting() throws java.io.IOException, InterruptedException, ProcessAlreadyCompletedException {
        long starttime = System.currentTimeMillis();
	/*
	while(true)
	{
        try {
            ServerSocket ss = new ServerSocket(3005);
            Socket soc = ss.accept();
            DataInputStream dis
                = new DataInputStream(soc.getInputStream());
            String str = (String)dis.readUTF();
            ss.close();
            if(str.equals(meetingId))
            {
				break;
			}
        }
        catch (Exception e) {
        }
	}*/
        ResponseProcess rs = new ResponseProcess();
        var task = CompletableFuture.runAsync(() -> {
            while (!rs.getStatus()) {
                Toolkit.getDefaultToolkit().beep();
                try {
                    Thread.sleep(4500);
                } catch (InterruptedException ex) {
                }
            }
        });
        while (true) {
            int result = JOptionPane.showConfirmDialog(null, "Did the meeting start?", "Meeting prompt",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                rs.setCompleted();
                break;
            }
            Thread.sleep(5000);
        }


        var task1 = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(40 * 60 * 1000);
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            this.meetingEnded = true;
        });
        long totalSeconds = 40 * 60, timeLeft = 1;
        while (!meetingEnded) {
            timeLeft = (totalSeconds - (System.currentTimeMillis() - starttime) / 1000);
            System.out.print("\rSTATUS : ONGOING ,Time left for meeting to end : " + timeLeft + "    ");
            if (timeLeft <= 0)
                break;
        }
        int result = JOptionPane.showConfirmDialog(null, "Do you want to rejoin meeting?", name + " ended!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            System.out.print("\rSTATUS : REJOINED on " + getCurrentHour() + ":" + getCurrentMinute());
        }

    }

    void waitUntil(Date schTime, String weekdays) throws InterruptedException, java.io.IOException, ProcessAlreadyCompletedException {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        String currentDay = (new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date.getTime()));
        boolean isEmpty = weekdays.equals(""), dayContained = weekdays.contains(currentDay);
        boolean weekdayNeg = false;
        if (!isEmpty && !dayContained)
            weekdayNeg = true;
        if (weekdayNeg) {
            ResponseProcess rs = new ResponseProcess();
            var task = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ex) {
                }
                if (!rs.getStatus())
                    System.exit(0);
            });
            int result = JOptionPane.showConfirmDialog(null, "Exiting due to no schedules today, join meeting?", name + " schedule",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            rs.setCompleted();
            if (!(result == JOptionPane.YES_OPTION)) {
                System.exit(0);
            }
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            waitPostMeeting();
            System.exit(0);
        }
        if (isafterTime(schTime)) {
            ResponseProcess rs = new ResponseProcess();
            var task = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ex) {
                }
                if (!rs.getStatus())
                    System.exit(0);
            });
            int result = JOptionPane.showConfirmDialog(null, "Exiting due to having elapsed scheduled time, rejoin meeting?", name + " ended!",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            rs.setCompleted();
            if (!(result == JOptionPane.YES_OPTION)) {
                System.exit(0);
            }
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            waitPostMeeting();
            System.exit(0);
        }
        JOptionPane.showMessageDialog(null, "Waiting for scheduled meeting.", name + " meeting scheduled", JOptionPane.WARNING_MESSAGE);
        String fullStop = ".  ";
        while (isbeforeTime(schTime)) {
            System.out.print("\rSTATUS : Waiting for meeting start" + fullStop);
            if (fullStop.lastIndexOf('.') == 0)
                fullStop = ".. ";
            else if (fullStop.lastIndexOf('.') == 1)
                fullStop = "...";
            else
                fullStop = ".  ";
            Thread.sleep(500);
        }
    }
}

class meetingDetailsInvalidException extends Exception {
    public meetingDetailsInvalidException(String message) {
        super(message);
    }
}

class ProcessAlreadyCompletedException extends Exception {
    public ProcessAlreadyCompletedException(String message) {
        super(message);
    }
}

class ResponseProcess {
    private boolean complete = false;

    public void setCompleted() throws ProcessAlreadyCompletedException {
        if (complete)
            throw new ProcessAlreadyCompletedException("Process already marked as completed!");
        complete = true;
    }

    public boolean getStatus() {
        return complete;
    }
}