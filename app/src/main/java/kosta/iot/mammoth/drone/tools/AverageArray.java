package kosta.iot.mammoth.drone.tools;

/**
 * Created by kosta on 2016-06-22.
 */
public class AverageArray {
    private int[] array;
    private int cnt;
    private int size;

    public AverageArray(final int size){
        this.array = new int[size];
        this.cnt = 0;
        this.size = size;
    }

    public void add(int value){
        this.array[cnt%size] = value;
        if(++cnt >= size)
            cnt=0;
    }

    public int getAverage() {
        int tmp = 0;

        for (int i = 0; i < this.size; i++)
            tmp += this.array[i];

        return tmp/this.size;
    }

    public int getSum(){
        int tmp = 0;

        for (int i = 0; i < this.size; i++)
            tmp += this.array[i];

        return Math.abs(tmp);
    }

    public int getSumNotABS(){
        int tmp = 0;

        for (int i = 0; i < this.size; i++)
            tmp += this.array[i];

        return tmp;
    }

    //배열의 값에서 변화량 구하기
    public int getRate(){
        int temp = 0;

        for (int i = 0; i < this.size - 1; i++) {
            temp += (array[i+1] - array[i]);
        }

        return temp;
    }
}
