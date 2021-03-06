package com.gmail.parusovvadim.audioplayer;

public class Track implements NodeDirectory
{
    private String name;
    private int number = -1;
    private int parentNumber = -1;
    private String m_path;

    Track(String name)
    {
        this.name = name;
    }

    void setPath(String path)
    {
        this.m_path = path;
    }

    public void setNumber(int number)
    {
        this.number = number;
    }

    void setParentNumber(int parentNumber)
    {
        this.parentNumber = parentNumber;
    }

    @Override
    public void SetName(String name)
    {
        this.name = name;
    }

    @Override
    public String GetName() {
        return name;
    }

    @Override
    public String GetPathDir() {
        return m_path;
    }

    @Override
    public int GetNumber() {
        return number;
    }

    @Override
    public int GetParentNumber() {
        return parentNumber;
    }

    @Override
    public int GetNumberTracks() {
        return -1;
    }

    @Override
    public boolean IsFolder() {
        return false;
    }

    @Override
    public boolean IsFolderUp() {
        return false;
    }
}
