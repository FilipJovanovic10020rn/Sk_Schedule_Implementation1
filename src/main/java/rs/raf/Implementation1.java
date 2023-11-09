package rs.raf;

import rs.raf.classes.ClassLecture;
import rs.raf.classes.Classroom;
import rs.raf.classes.Schedule;
import rs.raf.classes.Term;
import rs.raf.exceptions.*;
import rs.raf.schedule_management.ClassSchedule;
import rs.raf.schedule_management.ScheduleManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Implementation1 implements ClassSchedule {

    static {
        ScheduleManager.registerClassScheduler(new Implementation1());
    }

    @Override
    public void createClass(Schedule schedule, int startTime, int duration, String classroomName, String lectureName, String professor, Date fromDate, Date toDate)
            throws DatesException,DurationException,ClassroomDoesntExistException,TermTakenException,WrongStartTimeException, InternalError {

        if(schedule.getStartHours()>startTime || schedule.getEndHours()<startTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }

        if(fromDate.before(schedule.getStartDate() ) || toDate.after(schedule.getEndDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }

        if(duration<1){
            throw new DurationException("Trajanje mora biti minimum 1");
        }

        boolean flag = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(classroomName)){
                flag = true;
            }
        }
        if(!flag)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int count = 0;

        List<Term> termini = new ArrayList<>();

        for(Map.Entry<Term, ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i =0 ;i<duration; i++){
                if(entry.getKey().getDate().equals(fromDate) && entry.getKey().getClassroom().getName().equals(classroomName)
                        && entry.getKey().getStartTime() == startTime+i){
                    if(entry.getValue()==null){
                        count++;
                        termini.add(entry.getKey());
                    }
                }
                if(count==duration)
                    break;
            }
            if(count==duration) {
                if(termini.isEmpty()){
                    throw new InternalError("Greska u bazi");
                }
                ClassLecture cl = new ClassLecture(lectureName, professor, startTime, duration, fromDate, toDate);
                for(Term t : termini){
                    schedule.getScheduleMap().put(t,cl);
                }
                return;
            }
        }
        if(count!=duration)
        {
            throw new TermTakenException("ne postoji slobodan termin");
        }
    }


    @Override
    public void RemoveClass(Schedule schedule,Date date,Date toDate, int startTime, String classroomName, String lectureName)
            throws WrongStartTimeException,DatesException, WrongDateException,WrongLectureNameException, ClassroomDoesntExistException, ClassLectureDoesntExistException
    {

        if(schedule.getStartHours()>startTime || schedule.getEndHours()<startTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }
        if(date.before(schedule.getStartDate()) || date.before(schedule.getStartDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }
        boolean flag = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(classroomName)){
                flag = true;
            }
        }
        if(!flag)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int duration = 0;


        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            if(entry.getKey().getDate().equals(date) && entry.getKey().getClassroom().getName().equals(classroomName)
                    && entry.getKey().getStartTime() == startTime && entry.getValue().getClassName().equals(lectureName)){
                duration = entry.getValue().getDuration();
            }
        }
        if(duration==0)
        {
            throw new ClassLectureDoesntExistException("ne postoji cas sa zadatim podacima");
        }
        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i = 0; i<duration; i++){
                if(entry.getKey().getDate().equals(date) && entry.getKey().getClassroom().getName().equals(classroomName)
                        && entry.getKey().getStartTime() == startTime+i)
                {
                    schedule.getScheduleMap().put(entry.getKey(),null);
                }
            }
        }
    }


    @Override
    public void RescheduleClass(Schedule schedule, Date oldDate, Date oldToDate, int oldStartTime, String oldClassroomName, String lectureName,
                                Date newDate,Date newToDate, int newStartTime, String newClassroomName)
            throws DatesException,ClassroomDoesntExistException,WrongStartTimeException,WrongDateException,WrongLectureNameException,WrongClassroomNameException, TermTakenException{

        if(schedule.getStartHours()>oldStartTime || schedule.getEndHours()<oldStartTime || schedule.getStartHours()>newStartTime || schedule.getEndHours()<newStartTime){
            throw new WrongStartTimeException("Vreme koje ste dali je van radnih sati");
        }
        if(oldDate.before(schedule.getStartDate()) || oldDate.after(schedule.getEndDate()) || newDate.before(schedule.getStartDate()) || newDate.after(schedule.getEndDate())){
            throw new DatesException("Datum termina mora biti od: "+ schedule.getStartDate() + " do " + schedule.getEndDate());
        }
        boolean flag1 = false;
        boolean flag2 = false;
        for(Classroom classroom : schedule.getClassrooms()){
            if(classroom.getName().equals(oldClassroomName)){
                flag1 = true;
            }
            if(classroom.getName().equals(newClassroomName)){
                flag2 = true;
            }
        }
        if(!flag1 || !flag2)
            throw new ClassroomDoesntExistException("Ne postoji ucionica sa ovim parametrima");

        int duration = 0;
        ClassLecture cl = null;

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            if(entry.getKey().getDate().equals(oldDate) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                    && entry.getKey().getStartTime() == oldStartTime && entry.getValue().getClassName().equals(lectureName)){
                duration = entry.getValue().getDuration();
                cl = entry.getValue();
            }
        }
        if(cl==null){
            throw new ClassLectureDoesntExistException("ne postoji cas sa zadatim podacima");
        }

        int count = 0;

        List<Term> termini = new ArrayList<>();

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i =0 ;i<duration; i++){
                if(entry.getKey().getDate().equals(newDate) && entry.getKey().getClassroom().getName().equals(newClassroomName)
                        && entry.getKey().getStartTime() == newStartTime+i){
                    if(entry.getValue()==null){
                        count++;
                        termini.add(entry.getKey());
                    }
                }
                if(count==duration)
                    break;
            }
            if(count==duration) {
                if(termini.isEmpty()){
                    throw new InternalError("Greska u bazi");
                }
                for(Term t : termini){
                    schedule.getScheduleMap().put(t,cl);
                }
                return;
            }
        }
        if(count!=duration)
        {
            throw new TermDoesntExistException("ne postoji slobodan termin");
        }

        for(Map.Entry<Term,ClassLecture> entry : schedule.getScheduleMap().entrySet()){
            for(int i = 0; i<duration; i++){
                if(entry.getKey().getDate().equals(oldDate) && entry.getKey().getClassroom().getName().equals(oldClassroomName)
                        && entry.getKey().getStartTime() == oldStartTime+i)
                {
                    schedule.getScheduleMap().put(entry.getKey(),null);
                }
            }
        }
    }
}
