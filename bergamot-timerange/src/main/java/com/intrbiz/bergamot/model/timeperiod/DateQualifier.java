package com.intrbiz.bergamot.model.timeperiod;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Calendar;

import com.intrbiz.bergamot.model.timeperiod.util.Month;

public class DateQualifier extends ComposedTimeRange
{
    private static final long serialVersionUID = 1L;
    
    private int day;

    private Month month;

    private int year;

    public DateQualifier()
    {
        super();
    }

    public DateQualifier(int day, Month month, int year)
    {
        super();
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public DateQualifier(int day, Month month, int year, TimeRange... ranges)
    {
        super(ranges);
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public Month getMonth()
    {
        return month;
    }

    public void setMonth(Month month)
    {
        this.month = month;
    }

    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        this.day = day;
    }

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    @Override
    public boolean isInTimeRange(Calendar calendar)
    {
        return this.year == calendar.get(Calendar.YEAR) && calendar.get(Calendar.MONTH) == this.month.getMonth() && this.day == calendar.get(Calendar.DAY_OF_MONTH) && super.isInTimeRange(calendar);
    }

    @Override
    public LocalDateTime computeNextStartTime(Clock clock)
    {
        LocalDateTime next = super.computeNextStartTime(clock);
        if (next == null) return null;
        next = next.withYear(this.year).withMonth(this.month.getMonth() + 1).withDayOfMonth(this.day);
        // check the date is valid
        return (next.isAfter(LocalDateTime.now(clock))) ? next : null;
    }

    public String toString()
    {
        int mn = (this.month.ordinal() + 1);
        return this.year + "-" + (mn < 10 ? "0" : "") + mn + "-" + (this.day < 10 ? "0" : "") + this.day + " " + super.toString();
    }
}
