

    public class OclDate : IComparable
    {
        long time = 0;
        static long systemTime = 0;
        int year = 0;
        int month = 0;
        int day = 0;
        int weekday = 0;
        int hour = 0;
        int minute = 0;
        int second = 0;

        const long YEARMS = 31536000000L;
        const long MONTHMS = 2628000000L;
        const long DAYMS = 86400000L;
        const long HOURMS = 3600000L;
        const long MINUTEMS = 60000L;
        const long SECONDMS = 1000L;

        private static long getUnixTime(DateTime dd)
        {
            DateTimeOffset doff = new DateTimeOffset(dd);
            return doff.ToUnixTimeMilliseconds();
        }

        public static OclDate newOclDate()
        {
            DateTime dd = DateTime.Now;
            OclDate d = new OclDate();
            d.time = OclDate.getUnixTime(dd);
            d.year = dd.Year;
            d.month = dd.Month;
            d.day = dd.Day;
            d.weekday = (int)dd.DayOfWeek;
            d.hour = dd.Hour;
            d.minute = dd.Minute;
            d.second = dd.Second;
            OclDate.systemTime = d.time;
            return d;
        }

        public static OclDate newOclDate_Time(long t)
        {
            DateTime dd = DateTimeOffset.FromUnixTimeMilliseconds(t).DateTime;
            OclDate d = new OclDate();
            d.time = t;
            d.year = dd.Year;
            d.month = dd.Month;
            d.day = dd.Day;
            d.weekday = (int)dd.DayOfWeek;
            d.hour = dd.Hour;
            d.minute = dd.Minute;
            d.second = dd.Second;

            return d;
        }

        public static OclDate newOclDate_YMD(int y, int m, int d)
        {
            DateTime dt = new DateTime(y,m,d);
            OclDate dd = new OclDate();
            dd.time = OclDate.getUnixTime(dt);
            dd.year = y;
            dd.month = m;
            dd.day = d;
            dd.weekday = (int)dt.DayOfWeek;
            dd.hour = 0;
            dd.minute = 0;
            dd.second = 0;

            return dd;
        }

        public static OclDate newOclDate_YMDHMS(int y, int m, int d, int h, int mn, int sec)
        {
            DateTime dt = new DateTime(y, m, d, h, mn, sec);
            OclDate dd = new OclDate();
            dd.time = OclDate.getUnixTime(dt);
            dd.year = y;
            dd.month = m;
            dd.day = d;
            dd.weekday = (int)dt.DayOfWeek;
            dd.hour = h;
            dd.minute = mn;
            dd.second = sec;

            return dd;
        }

        public static OclDate newOclDate_String(string s)
        { ArrayList items = SystemTypes.allMatches(s, "[0-9]+");
            OclDate d = new OclDate();
            if (items.Count >= 3)
            {
                d.year = int.Parse((string)items[0]);
                d.month = int.Parse((string)items[1]);
                d.day = int.Parse((string)items[2]);
            }
            if (items.Count >= 6)
            {
                d.hour = int.Parse((string)items[3]);
                d.minute = int.Parse((string)items[4]);
                d.second = int.Parse((string)items[5]);
            }
            return newOclDate_YMDHMS(d.year, d.month, d.day, d.hour, d.minute, d.second); 
        }

        public void setTime(long t)
        {
            DateTime dd = DateTimeOffset.FromUnixTimeMilliseconds(t).DateTime;
            time = t;
            year = dd.Year;
            month = dd.Month;
            day = dd.Day;
            weekday = (int)dd.DayOfWeek;
            hour = dd.Hour;
            minute = dd.Minute;
            second = dd.Second;
        }

        public long getTime()
        { return time; }

        public static long getSystemTime()
        {
            DateTime dd = DateTime.Now;
            OclDate.systemTime = OclDate.getUnixTime(dd);
            return OclDate.systemTime;
        }


        public int getYear()
        { return year; }

        public int getMonth()
        { return month; }

        public int getDate()
        { return day; }

        public int getDay()
        { return weekday; }

        public int getHours()
        { return hour; }

        public int getHour()
        { return hour; }

        public int getMinutes()
        { return minute; }

        public int getMinute()
        { return minute; }

        public int getSeconds()
        { return second; }

        public int getSecond()
        { return second; }

        public OclDate addYears(int y)
        {
            long newtime = time + y * YEARMS;
            return newOclDate_Time(newtime);
        }

        public OclDate addMonths(int m)
        {
            long newtime = time + m * MONTHMS;
            return newOclDate_Time(newtime);
        }

        public OclDate addDays(int d)
        {
            long newtime = time + d * DAYMS;
            return newOclDate_Time(newtime);
        }

        public OclDate addHours(int h)
        {
            long newtime = time + h * HOURMS;
            return newOclDate_Time(newtime);
        }

        public OclDate addMinutes(int m)
        {
            long newtime = time + m * MINUTEMS;
            return newOclDate_Time(newtime);
        }

        public OclDate addSeconds(int s)
        {
            long newtime = time + s * SECONDMS;
            return newOclDate_Time(newtime);
        }

        public long yearDifference(OclDate d)
        {
            long newtime = time - d.time;
            return newtime / YEARMS;
        }

        public long monthDifference(OclDate d)
        {
            long newtime = time - d.time;
            return newtime / MONTHMS;
        }

        public long dayDifference(OclDate d)
        {
            long newtime = time - d.time;
            return newtime / DAYMS;
        }

        public long hourDifference(OclDate d)
        {
            long newtime = time - d.time;
            return newtime / HOURMS;
        }

        public long minuteDifference(OclDate d)
        {
            long newtime = time - d.time;
            return newtime / MINUTEMS;
        }

        public long secondDifference(OclDate d)
        {
            long newtime = time - d.time;
            return newtime / SECONDMS;
        }

        public int CompareTo(object obj)
        {
            if (obj is OclDate)
            {
                OclDate dd = (OclDate)obj;
                if (time < dd.time) { return -1; }
                if (time > dd.time) { return 1; }
                return 0;
            }
            return 0;
        } 
          
        public bool dateBefore(OclDate d)
        {
            bool result = false;
            if (time < d.time)
            {
                result = true;
            }
            else
            {
                result = false;
            }
            return result;
        }


        public bool dateAfter(OclDate d)
        {
            bool result = false;
            if (time > d.time)
            {
                result = true;
            }
            else
            {
                result = false;
            }
            return result;
        }

        public override string ToString()
        {
            return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second; 
            // DateTime dd = DateTimeOffset.FromUnixTimeMilliseconds(time).DateTime;
            // return dd.ToString();
        }
    }

